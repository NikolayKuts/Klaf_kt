package com.kuts.klaf.networking.klafServer

import com.kuts.klaf.server.contract.KlafServerClientMessage
import com.kuts.klaf.server.contract.KlafServerMessage
import com.kuts.klaf.server.contract.KlafServerReadyMessage
import com.kuts.klaf.server.contract.WordInsightsGenerateRequest
import com.kuts.klaf.server.contract.WordInsightsGeneratedMessage
import com.kuts.klaf.server.contract.WordMeaningInsightsDto
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO as ClientCIO
import io.ktor.client.plugins.websocket.WebSockets as ClientWebSockets
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.routing
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private val sessionTestJson = Json { classDiscriminator = "type" }

private suspend fun withSessionServer(
    handler: suspend DefaultWebSocketServerSession.() -> Unit,
    test: suspend (KlafServerSession) -> Unit,
) {
    val port = ServerSocket(0).use { it.localPort }
    val server = embeddedServer(CIO, host = "127.0.0.1", port = port) {
        install(WebSockets)
        routing { webSocket("/ws") { handler() } }
    }.start(wait = false)
    val client = HttpClient(ClientCIO) { install(ClientWebSockets) }
    val session = KlafServerSession("127.0.0.1", port, false, client)
    try {
        test(session)
    } finally {
        withTimeout(2_000) { session.disconnect() }
        client.close()
        server.stop(0, 2_000)
    }
}

class KlafServerSessionLifecycleTest {

    @Test
    fun closeBeforeReadyFailsPromptlyInsteadOfWaitingForHandshakeTimeout() = runBlocking {
        withSessionServer(handler = { close() }) { session ->
            assertFailsWith<IllegalStateException> {
                withTimeout(2_000) { session.connect() }
            }
        }
    }

    @Test
    fun cancellingBeforeReadyDoesNotDeadlockReaderCleanup() = runBlocking {
        val connected = CompletableDeferred<Unit>()
        withSessionServer(handler = { connected.complete(Unit); awaitCancellation() }) { session ->
            val attempt = async { session.connect() }
            connected.await()
            withTimeout(2_000) { attempt.cancelAndJoin() }
        }
    }

    @Test
    fun cleanCloseReplaysPendingRequestOnNextConnection() = runBlocking {
        val connections = AtomicInteger()
        withSessionServer(handler = {
            val connection = connections.incrementAndGet()
            send(sessionTestJson.encodeToString<KlafServerMessage>(KlafServerReadyMessage()))
            val frame = incoming.receive() as Frame.Text
            val request = sessionTestJson.decodeFromString<KlafServerClientMessage>(frame.readText())
            if (connection == 1) {
                close()
            } else {
                send(sessionTestJson.encodeToString<KlafServerMessage>(WordInsightsGeneratedMessage(
                    request.requestId, WordMeaningInsightsDto("word", "en", emptyList()),
                )))
                awaitCancellation()
            }
        }) { session ->
            val response = withTimeout(5_000) { session.request(WordInsightsGenerateRequest("req-replay", "word")) }
            assertEquals("req-replay", (response as WordInsightsGeneratedMessage).requestId)
            assertEquals(2, connections.get())
        }
    }
}
