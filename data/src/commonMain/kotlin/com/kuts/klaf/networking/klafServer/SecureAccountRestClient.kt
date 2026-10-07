package com.kuts.klaf.networking.klafServer

import com.kuts.domain.managers.AccountPasswordPolicy
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val AUTH_REQUEST_TIMEOUT_MILLIS = 15_000L

@Serializable
private data class NewAccountBody(
    val email: String,
    val password: String,
    val deviceId: String,
    val deviceName: String,
    val platform: String,
)

@Serializable
private data class EnrollmentResponse(val requestId: String, val status: String)

@Serializable
private data class AuthenticationErrorBody(val code: String)

@Serializable
private data class EnrollmentStatusBody(val requestId: String)

@Serializable
private data class EnrollmentStatusResponse(val status: String)

@Serializable
private data class EnrollmentCompletionBody(val requestId: String, val password: String)

@Serializable
private data class RefreshCredentialsBody(val refreshToken: String, val operationId: String)

@Serializable
private data class PasswordResetRequest(
    val email: String,
    val token: String,
    val password: String,
)

@Serializable
internal data class SecureLoginTokens(val accessToken: String, val refreshToken: String)

internal data class PendingEnrollment(
    val requestId: String,
    val status: EnrollmentApprovalStatus = EnrollmentApprovalStatus.AWAITING_APPROVAL,
)

internal enum class EnrollmentKind { ACCOUNT, DEVICE }

internal enum class EnrollmentApprovalStatus { AWAITING_APPROVAL, APPROVED, EXPIRED }

internal sealed interface SecureSignInResult {
    data class Pending(val requestId: String) : SecureSignInResult
    data class Authenticated(val tokens: SecureLoginTokens) : SecureSignInResult
}

internal interface SecureAccountGateway {
    suspend fun submitRegistration(
        email: String,
        password: String,
        device: AccountDevice,
    ): PendingEnrollment

    suspend fun signIn(
        email: String,
        password: String,
        device: AccountDevice,
    ): SecureSignInResult

    suspend fun enrollmentStatus(requestId: String): EnrollmentApprovalStatus
    suspend fun completeEnrollment(
        requestId: String,
        password: String,
        kind: EnrollmentKind,
    ): SecureLoginTokens

    suspend fun refresh(refreshToken: String, operationId: String): SecureLoginTokens
    suspend fun logout(accessToken: String)
    suspend fun resetPassword(
        email: String,
        token: String,
        password: String,
    ) {
        throw UnsupportedOperationException("Password reset is not supported by this gateway")
    }
}

/** Authentication-only REST client; no legacy passwordless route is called here. */
internal class SecureAccountRestClient(
    baseUrl: String,
    private val httpClient: HttpClient,
    private val proofs: DpopProofFactory,
    private val requestTimeoutMillis: Long = AUTH_REQUEST_TIMEOUT_MILLIS,
) : SecureAccountGateway {

    private val endpointBase = baseUrl.trimEnd('/')
    private val json = Json { ignoreUnknownKeys = true }

    init {
        require(requestTimeoutMillis > 0)
        requireSafeAuthOrigin(endpointBase)
    }

    override suspend fun submitRegistration(
        email: String,
        password: String,
        device: AccountDevice,
    ): PendingEnrollment = klafServerRequest(requestTimeoutMillis) {
        require(AccountPasswordPolicy.isValid(password))
        val (_, body) = postWithProof("/api/v1/auth/registrations", json.encodeToString(
            NewAccountBody(email, password, device.id, device.name, device.platform),
        ))
        val enrollment = json.decodeFromString<EnrollmentResponse>(body)
        val status = EnrollmentApprovalStatus.entries.firstOrNull { it.name == enrollment.status }
        if (status == null || status == EnrollmentApprovalStatus.EXPIRED ||
            enrollment.requestId.length !in 16..128) {
            throw invalidResponse()
        }
        PendingEnrollment(enrollment.requestId, status)
    }

    override suspend fun signIn(
        email: String,
        password: String,
        device: AccountDevice,
    ): SecureSignInResult = klafServerRequest(requestTimeoutMillis) {
        require(AccountPasswordPolicy.isValid(password))
        val (status, body) = postWithProof("/api/v1/auth/sign-in", json.encodeToString(
            NewAccountBody(email, password, device.id, device.name, device.platform),
        ))
        when (status) {
            200 -> SecureSignInResult.Authenticated(validatedTokens(body))
            202 -> {
                val enrollment = json.decodeFromString<EnrollmentResponse>(body)
                if (enrollment.status != "AWAITING_APPROVAL" || enrollment.requestId.length !in 16..128) {
                    throw invalidResponse()
                }
                SecureSignInResult.Pending(enrollment.requestId)
            }
            else -> throw invalidResponse()
        }
    }

    override suspend fun enrollmentStatus(requestId: String): EnrollmentApprovalStatus =
        klafServerRequest(requestTimeoutMillis) {
            require(requestId.length in 16..128)
            val (_, body) = postWithProof("/api/v1/auth/enrollments/status",
                json.encodeToString(EnrollmentStatusBody(requestId)))
            val status = json.decodeFromString<EnrollmentStatusResponse>(body).status
            EnrollmentApprovalStatus.entries.firstOrNull { it.name == status } ?: throw invalidResponse()
        }

    override suspend fun completeEnrollment(
        requestId: String,
        password: String,
        kind: EnrollmentKind,
    ): SecureLoginTokens = klafServerRequest(requestTimeoutMillis) {
        require(requestId.length in 16..128 && AccountPasswordPolicy.isValid(password))
        val path = when (kind) {
            EnrollmentKind.ACCOUNT -> "/api/v1/auth/registrations/complete"
            EnrollmentKind.DEVICE -> "/api/v1/auth/devices/complete"
        }
        val (_, body) = postWithProof(path, json.encodeToString(EnrollmentCompletionBody(requestId, password)))
        validatedTokens(body)
    }

    override suspend fun refresh(refreshToken: String, operationId: String): SecureLoginTokens =
        klafServerRequest(requestTimeoutMillis) {
            require(refreshToken.isNotBlank() && operationId.length in 16..128)
            val (_, body) = postWithProof("/api/v1/auth/refresh",
                json.encodeToString(RefreshCredentialsBody(refreshToken, operationId)))
            validatedTokens(body)
        }

    override suspend fun logout(accessToken: String): Unit = klafServerRequest(requestTimeoutMillis) {
        require(accessToken.isNotBlank())
        val (status, _) = postWithProof("/api/v1/auth/logout", "", accessToken)
        if (status != 204) throw invalidResponse()
    }

    override suspend fun resetPassword(
        email: String,
        token: String,
        password: String,
    ): Unit = klafServerRequest(requestTimeoutMillis) {
        require(AccountPasswordPolicy.isValid(password) && token.isNotBlank())
        val (status, _) = postWithProof("/api/v1/auth/password/reset",
            json.encodeToString(PasswordResetRequest(email, token, password)))
        if (status != 204) throw invalidResponse()
    }

    private fun validatedTokens(body: String): SecureLoginTokens = json.decodeFromString<SecureLoginTokens>(body).also {
        if (it.accessToken.isBlank() || it.refreshToken.isBlank()) throw invalidResponse()
    }

    private fun invalidResponse() = com.kuts.domain.managers.AccountOperationException(
        com.kuts.domain.managers.AccountFailure.INVALID_RESPONSE)

    private suspend fun postWithProof(
        path: String,
        body: String,
        accessToken: String? = null,
    ): Pair<Int, String> {
        val url = endpointBase + path
        var nonce: String? = null
        for (attempt in 0..1) {
            val response = httpClient.post(url) {
                contentType(ContentType.Application.Json)
                headers.append("DPoP", proofs.create("POST", url, nonce, accessToken))
                if (accessToken != null) headers.append("Authorization", "DPoP $accessToken")
                setBody(body)
            }
            val responseBody = response.bodyAsText()
            val code = runCatching { json.decodeFromString<AuthenticationErrorBody>(responseBody).code }.getOrNull()
            val challenge = response.headers["DPoP-Nonce"]
            if (attempt == 0 && response.status.value == 401 && code == "DPOP_NONCE_REQUIRED" &&
                challenge != null && challenge.length in 16..128
            ) {
                nonce = challenge
                continue
            }
            if (response.status.value !in 200..299) {
                val retryAfter = response.headers["Retry-After"]?.toIntOrNull()?.takeIf { it in 1..900 }
                throw AccountHttpException(response.status.value, code ?: "HTTP_ERROR", retryAfter)
            }
            return response.status.value to responseBody
        }
        throw com.kuts.domain.managers.AccountOperationException(
            com.kuts.domain.managers.AccountFailure.INVALID_RESPONSE)
    }
}
