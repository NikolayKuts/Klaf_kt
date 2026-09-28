package com.kuts.klaf.networking.klafServer

import com.kuts.klaf.server.contract.SyncEventMessage
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.http.encodeURLParameter
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json

private const val EVENT_CONNECT_TIMEOUT_MILLIS = 5_000L

class KlafServerSyncEventConnector(
    baseUrl: String,
    private val httpClient: HttpClient,
) : SyncEventConnector {

    private val socketBase = when {
        baseUrl.startsWith("http://") -> "ws://${baseUrl.removePrefix("http://").trimEnd('/')}"
        baseUrl.startsWith("https://") -> "wss://${baseUrl.removePrefix("https://").trimEnd('/')}"
        else -> throw IllegalArgumentException("Sync event server URL must use HTTP or HTTPS")
    }
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        classDiscriminator = "type"
    }

    override suspend fun open(email: String, deviceId: String): SyncEventConnection {
        val session = withTimeout(EVENT_CONNECT_TIMEOUT_MILLIS) {
            httpClient.webSocketSession(urlString = "$socketBase/sync-events?" +
                "email=${email.encodeURLParameter()}&deviceId=${deviceId.encodeURLParameter()}")
        }
        return KtorSyncEventConnection(session, json)
    }
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
