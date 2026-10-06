package com.kuts.klaf.networking.klafServer

import com.kuts.domain.managers.AccountFailure
import com.kuts.domain.managers.AccountOperationException
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val ACCOUNT_REQUEST_TIMEOUT_MILLIS = 15_000L

internal fun accountHttpFailure(status: Int, code: String): AccountFailure = when {
    status >= 500 -> AccountFailure.SERVER
    code == "ACCOUNT_NOT_FOUND" -> AccountFailure.ACCOUNT_NOT_FOUND
    code == "ACCOUNT_EXISTS" -> AccountFailure.ACCOUNT_EXISTS
    code == "DEVICE_NOT_REGISTERED" || code == "DEVICE_ALREADY_REGISTERED" -> AccountFailure.DEVICE_REGISTRATION
    code == "SIGN_IN_REQUIRED" || code == "AUTHENTICATION_REQUIRED" -> AccountFailure.SIGN_IN_REQUIRED
    code == "INVALID_CREDENTIALS" -> AccountFailure.INVALID_CREDENTIALS
    code == "RESET_TOKEN_INVALID" -> AccountFailure.RESET_TOKEN_INVALID
    code == "REGISTRATION_NOT_APPROVED" || code == "DEVICE_NOT_APPROVED" -> AccountFailure.APPROVAL_PENDING
    code == "INVALID_DEVICE_PROOF" -> AccountFailure.DEVICE_PROOF
    code == "AUTH_BUSY" -> AccountFailure.SERVER_BUSY
    code == "AUTH_THROTTLED" || code == "AUTH_RATE_LIMITED" -> AccountFailure.THROTTLED
    code == "INVALID_REQUEST" -> AccountFailure.INVALID_REQUEST
    else -> AccountFailure.INVALID_RESPONSE
}

@Serializable
data class AccountDevice(val id: String, val name: String, val platform: String)

data class CreatedAccount(val email: String, val deviceId: String)

@Serializable
private data class AccountRegistrationRequest(
    val email: String,
    val deviceId: String,
    val deviceName: String,
    val platform: String,
)

@Serializable
private data class AccountCreationRequest(
    val email: String,
    val deviceId: String,
    val deviceName: String,
    val platform: String,
    val requestId: String? = null,
)

@Serializable
private data class AccountSignInRequest(val email: String, val deviceId: String)

@Serializable
private data class AccountResponse(val email: String, val deviceId: String)

@Serializable
private data class AccountStatusResponse(val code: String)

@Serializable
private data class AccountErrorBody(val code: String)

class AccountHttpException(
    val statusCode: Int,
    val errorCode: String,
    retryAfterSeconds: Int? = null,
) : AccountOperationException(accountHttpFailure(statusCode, errorCode), "Klaf account HTTP $statusCode: $errorCode",
    retryAfterSeconds = retryAfterSeconds)

class KlafServerAccountRestClient(
    baseUrl: String,
    private val httpClient: HttpClient,
    private val requestTimeoutMillis: Long = ACCOUNT_REQUEST_TIMEOUT_MILLIS,
) {

    private val endpointBase = baseUrl.trimEnd('/')
    private val json = Json { ignoreUnknownKeys = true }

    init {
        require(requestTimeoutMillis > 0)
        require(endpointBase.startsWith("http://") || endpointBase.startsWith("https://")) {
            "Account server URL must use HTTP or HTTPS"
        }
    }

    suspend fun signUp(email: String, device: AccountDevice, requestId: String? = null): CreatedAccount = accountRequest {
        val response = httpClient.post("$endpointBase/api/v1/accounts") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(
                AccountCreationRequest(email, device.id, device.name, device.platform, requestId),
            ))
        }
        val account = json.decodeFromString<AccountResponse>(response.successBody())
        if (account.email != email.trim().lowercase() || account.deviceId != device.id) {
            throw AccountOperationException(AccountFailure.INVALID_RESPONSE)
        }
        CreatedAccount(account.email, account.deviceId)
    }

    suspend fun signIn(email: String, deviceId: String) = accountRequest {
        httpClient.post("$endpointBase/api/v1/accounts/sign-in") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(AccountSignInRequest(email, deviceId)))
        }.successBody().also { body ->
            if (json.decodeFromString<AccountStatusResponse>(body).code != "SIGNED_IN") {
                throw AccountOperationException(AccountFailure.INVALID_RESPONSE)
            }
        }
    }

    suspend fun registerDevice(email: String, device: AccountDevice) = accountRequest {
        httpClient.post("$endpointBase/api/v1/accounts/devices") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(deviceRequest(email, device)))
        }.successBody().also { body ->
            if (json.decodeFromString<AccountStatusResponse>(body).code != "DEVICE_REGISTERED") {
                throw AccountOperationException(AccountFailure.INVALID_RESPONSE)
            }
        }
    }

    private fun deviceRequest(email: String, device: AccountDevice) = AccountRegistrationRequest(
        email = email,
        deviceId = device.id,
        deviceName = device.name,
        platform = device.platform,
    )

    private suspend fun <T> accountRequest(block: suspend () -> T): T = klafServerRequest(requestTimeoutMillis, block)

    private suspend fun HttpResponse.successBody(): String {
        val body = bodyAsText()
        if (status.value !in 200..299) {
            val code = runCatching { json.decodeFromString<AccountErrorBody>(body).code }
                .getOrDefault("HTTP_ERROR")
            throw AccountHttpException(status.value, code)
        }
        return body
    }
}
