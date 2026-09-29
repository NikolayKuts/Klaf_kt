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
        val response = httpClient.get("$endpointBase/api/v1/sync/bootstrap") {
            url {
                parameters.append("email", email)
                parameters.append("deviceId", deviceId)
            }
        }
        json.decodeFromString<SyncBootstrapResponse>(response.successBody())
    }

    suspend fun sync(request: SyncRequest): SyncResponse = klafServerRequest(SYNC_REQUEST_TIMEOUT_MILLIS) {
        val response = httpClient.post("$endpointBase/api/v1/sync") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(request))
        }
        json.decodeFromString<SyncResponse>(response.successBody())
    }

    suspend fun recentHistory(email: String, deviceId: String): SyncHistoryResponse = klafServerRequest(
        SYNC_REQUEST_TIMEOUT_MILLIS,
    ) {
        val response = httpClient.get("$endpointBase/api/v1/sync/history") {
            url {
                parameters.append("email", email)
                parameters.append("deviceId", deviceId)
            }
        }
        json.decodeFromString<SyncHistoryResponse>(response.successBody())
    }

    suspend fun confirmAppliedRevision(
        email: String,
        deviceId: String,
        revision: Long,
    ): SyncPositionConfirmationResponse = klafServerRequest(SYNC_REQUEST_TIMEOUT_MILLIS) {
        val response = httpClient.post("$endpointBase/api/v1/sync/confirm") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(SyncPositionConfirmationRequest(
                email = email,
                deviceId = deviceId,
                protocolVersion = 3,
                revision = revision,
            )))
        }
        json.decodeFromString<SyncPositionConfirmationResponse>(response.successBody())
    }

    private suspend fun HttpResponse.successBody(): String {
        val body = bodyAsText()
        if (status.value !in 200..299) {
            val code = runCatching { json.decodeFromString<SyncErrorBody>(body).code }
                .getOrDefault("HTTP_ERROR")
            throw SyncHttpException(status.value, code)
        }
        return body
    }
}
