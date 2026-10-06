package com.kuts.klaf.networking.klafServer

import com.kuts.domain.managers.AccountOperationException
import com.kuts.klaf.server.contract.SyncBootstrapResponse
import com.kuts.klaf.server.contract.SyncHistoryResponse
import com.kuts.klaf.server.contract.SyncPositionConfirmationRequest
import com.kuts.klaf.server.contract.SyncPositionConfirmationResponse
import com.kuts.klaf.server.contract.SyncRequest
import com.kuts.klaf.server.contract.SyncResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val SYNC_REQUEST_TIMEOUT_MILLIS = 15_000L

@Serializable
private data class SyncErrorBody(val code: String)

class SyncHttpException(
    val statusCode: Int,
    val errorCode: String,
) : AccountOperationException(accountHttpFailure(statusCode, errorCode), "Klaf sync HTTP $statusCode: $errorCode")

class KlafServerSyncRestClient(
    baseUrl: String,
    private val httpClient: HttpClient,
    private val signer: KlafAuthenticatedRequestSigner? = null,
) {

    private val endpointBase = baseUrl.trimEnd('/')
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        classDiscriminator = "type"
    }

    init {
        require(endpointBase.startsWith("http://") || endpointBase.startsWith("https://")) {
            "Sync server URL must use HTTP or HTTPS"
        }
    }

    suspend fun bootstrap(email: String, deviceId: String): SyncBootstrapResponse = klafServerRequest(
        SYNC_REQUEST_TIMEOUT_MILLIS,
    ) {
        val body = requestWithProof("GET", "/api/v1/sync/bootstrap", email) { proof ->
            httpClient.get("$endpointBase/api/v1/sync/bootstrap") {
                url {
                    parameters.append("email", email)
                    parameters.append("deviceId", deviceId)
                }
                proof?.let { headers.append("Authorization", it.authorization); headers.append("DPoP", it.dpop) }
            }
        }
        json.decodeFromString<SyncBootstrapResponse>(body)
    }

    suspend fun sync(request: SyncRequest): SyncResponse = klafServerRequest(SYNC_REQUEST_TIMEOUT_MILLIS) {
        val encoded = json.encodeToString(request)
        val body = requestWithProof("POST", "/api/v1/sync", request.email) { proof ->
            httpClient.post("$endpointBase/api/v1/sync") {
                contentType(ContentType.Application.Json)
                proof?.let { headers.append("Authorization", it.authorization); headers.append("DPoP", it.dpop) }
                setBody(encoded)
            }
        }
        json.decodeFromString<SyncResponse>(body)
    }

    suspend fun recentHistory(email: String, deviceId: String): SyncHistoryResponse = klafServerRequest(
        SYNC_REQUEST_TIMEOUT_MILLIS,
    ) {
        val body = requestWithProof("GET", "/api/v1/sync/history", email) { proof ->
            httpClient.get("$endpointBase/api/v1/sync/history") {
                url {
                    parameters.append("email", email)
                    parameters.append("deviceId", deviceId)
                }
                proof?.let { headers.append("Authorization", it.authorization); headers.append("DPoP", it.dpop) }
            }
        }
        json.decodeFromString<SyncHistoryResponse>(body)
    }

    suspend fun confirmAppliedRevision(
        email: String,
        deviceId: String,
        revision: Long,
    ): SyncPositionConfirmationResponse = klafServerRequest(SYNC_REQUEST_TIMEOUT_MILLIS) {
        val encoded = json.encodeToString(SyncPositionConfirmationRequest(
            email = email,
            deviceId = deviceId,
            protocolVersion = 3,
            revision = revision,
        ))
        val body = requestWithProof("POST", "/api/v1/sync/confirm", email) { proof ->
            httpClient.post("$endpointBase/api/v1/sync/confirm") {
                contentType(ContentType.Application.Json)
                proof?.let { headers.append("Authorization", it.authorization); headers.append("DPoP", it.dpop) }
                setBody(encoded)
            }
        }
        json.decodeFromString<SyncPositionConfirmationResponse>(body)
    }

    private suspend fun requestWithProof(
        method: String,
        path: String,
        email: String,
        send: suspend (AccessProofHeaders?) -> HttpResponse,
    ): String {
        var nonce: String? = null
        for (attempt in 0..1) {
            val proof = signer?.headers(email, method, endpointBase + path, nonce)
            val response = send(proof)
            val body = response.bodyAsText()
            val code = runCatching { json.decodeFromString<SyncErrorBody>(body).code }.getOrDefault("HTTP_ERROR")
            val challenge = response.headers["DPoP-Nonce"]
            if (signer != null && attempt == 0 && response.status.value == 401 &&
                code == "DPOP_NONCE_REQUIRED" && challenge != null && challenge.length in 16..128) {
                nonce = challenge
                continue
            }
            if (response.status.value !in 200..299) throw SyncHttpException(response.status.value, code)
            return body
        }
        throw AccountOperationException(com.kuts.domain.managers.AccountFailure.INVALID_RESPONSE)
    }
}
