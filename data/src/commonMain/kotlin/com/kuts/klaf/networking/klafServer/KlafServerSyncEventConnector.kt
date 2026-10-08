package com.kuts.klaf.networking.klafServer

import com.kuts.klaf.server.contract.SyncEventMessage
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.statement.bodyAsText
import io.ktor.http.encodeURLParameter
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val EVENT_CONNECT_TIMEOUT_MILLIS = 5_000L

@Serializable
private data class SyncEventChallengeError(val code: String)

class KlafServerSyncEventConnector(
    baseUrl: String,
    private val httpClient: HttpClient,
    private val signer: KlafAuthenticatedRequestSigner? = null,
) : SyncEventConnector {

    private val endpointBase = baseUrl.trimEnd('/')
    private val socketBase = klafServerWebSocketBaseUrl(baseUrl)
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        classDiscriminator = "type"
    }

    override suspend fun open(email: String, deviceId: String): SyncEventConnection {
        val path = eventPath(email, deviceId)
        val session = withTimeout(EVENT_CONNECT_TIMEOUT_MILLIS) {
            val nonce = signer?.let { requestConnectionNonce(email, deviceId) }
            val proof = signer?.headers(email, "GET", endpointBase + path, nonce)
            httpClient.webSocketSession(urlString = "$socketBase$path", block = {
                proof?.let { headers.append("Authorization", it.authorization); headers.append("DPoP", it.dpop) }
            })
        }
        return KtorSyncEventConnection(session, json)
    }

    internal suspend fun requestConnectionNonce(email: String, deviceId: String): String {
        val url = endpointBase + eventPath(email, deviceId)
        val proof = requireNotNull(signer).headers(email, "GET", url, null)
        val response = httpClient.get(url) {
            headers.append("Authorization", proof.authorization)
            headers.append("DPoP", proof.dpop)
        }
        val body = response.bodyAsText()
        val nonce = response.headers["DPoP-Nonce"]
        val code = runCatching { Json.decodeFromString<SyncEventChallengeError>(body).code }.getOrNull()
        if (response.status.value == 401 && code == "DPOP_NONCE_REQUIRED" &&
            nonce != null && nonce.length in 16..128
        ) return nonce
        throw SyncHttpException(response.status.value, code ?: "SYNC_EVENT_NONCE_UNAVAILABLE")
    }

    private fun eventPath(email: String, deviceId: String): String =
        "/sync-events?email=${email.encodeURLParameter()}&deviceId=${deviceId.encodeURLParameter()}"
}

private class KtorSyncEventConnection(
    private val session: DefaultClientWebSocketSession,
    private val json: Json,
) : SyncEventConnection {

    override suspend fun receive(): SyncEventMessage? {
        for (frame in session.incoming) {
            if (frame is Frame.Text) return json.decodeFromString<SyncEventMessage>(frame.readText())
        }
        return null
    }

    override suspend fun close() {
        session.close()
    }
}
