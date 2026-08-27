package com.kuts.klaf.networking.agentDriver

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.agentdriver.project.ktorclient.external.AssistantClientConnectionState
import org.agentdriver.project.ktorclient.external.AssistantClientEndpoint
import org.agentdriver.project.ktorclient.external.ClientAuthentication
import org.agentdriver.project.ktorclient.external.IAssistantClient
import org.agentdriver.project.ktorclient.external.KtorAssistantClient
import org.agentdriver.project.ktorclient.external.KtorAssistantClientEvent
import org.agentdriver.project.ktorclient.external.KtorAssistantClientEventListener
import org.agentdriver.project.protocol.ImageGenerationRequest
import org.agentdriver.project.protocol.TextGenerationRequest
import com.lib.lokdroid.core.logD
import com.lib.lokdroid.core.logE

/**
 * The app's connection to the assistant.
 *
 * Open only while the assistant is switched on: [switchOn] and [switchOff] follow the provider
 * switch, and nothing else opens the connection. Everything the app asks the assistant -- word
 * insights, mnemonic associations, mnemonic illustrations -- goes through [generateText] and
 * [generateImage] and shares this one connection.
 *
 * Staying connected is the SDK's job. It reconnects by itself after a drop, so a request only ever
 * has to open the connection when it is the first one; if the connection is down at that moment,
 * the request fails with a typed error the caller reports.
 */
class AgentDriverSession(
    serverHost: String,
    clientToken: String,
) {

    companion object {

        /** Cloudflare terminates TLS for the tunnel, so the client always reaches it here. */
        private const val CLOUDFLARE_HTTPS_PORT = 443
    }

    private val client: IAssistantClient = KtorAssistantClient(
        endpoint = AssistantClientEndpoint(
            host = serverHost.trim(),
            port = CLOUDFLARE_HTTPS_PORT,
            secure = true,
        ),
        // The AgentDriver server issued this and checks it itself, so the tunnel in front only has
        // to carry the connection rather than decide who may open one.
        authentication = ClientAuthentication.BearerToken(token = clientToken.trim()),
        eventListener = KtorAssistantClientEventListener { event ->
            when (event) {
                is KtorAssistantClientEvent.RequestSendFailed ->
                    logE("Agent Driver SDK failed to send ${event.messageType}: ${event.failure.describeForLog()}")

                is KtorAssistantClientEvent.MonitorConnectionFailed ->
                    logE("Agent Driver SDK connection monitor failed: ${event.failure.describeForLog()}")

                else -> logD("Agent Driver SDK event: $event")
            }
        },
    )

    val connectionState: StateFlow<AssistantClientConnectionState> get() = client.connectionState

    /** Where this client connects. Fixed for its lifetime, and part of what a session log states. */
    val endpoint: AssistantClientEndpoint get() = client.endpoint

    private val switchedOn = MutableStateFlow(value = false)
    private val connectMutex = Mutex()

    /** Opens the connection, and lets requests open it again after a drop the SDK cannot recover. */
    suspend fun switchOn() {
        switchedOn.value = true
        awaitOpenSession()
    }

    /** Closes the connection. Nothing opens it again until [switchOn]. */
    suspend fun switchOff() {
        switchedOn.value = false
        client.disconnect()
    }

    suspend fun generateText(request: TextGenerationRequest): String {
        awaitOpenSession()
        return client.generateText(request = request)
    }

    suspend fun generateImage(request: ImageGenerationRequest): ByteArray {
        awaitOpenSession()
        return client.generateImage(request = request).bytes
    }

    /**
     * Waits for an open connection, opening it if this is the first request.
     *
     * Serialized so that several features asking at once produce one connection rather than a race
     * between them; once connected, the fast path is a single state read.
     */
    private suspend fun awaitOpenSession() {
        check(value = switchedOn.value) { "The assistant is switched off." }

        if (connectionState.value is AssistantClientConnectionState.Connected) return

        connectMutex.withLock {
            when (connectionState.value) {
                is AssistantClientConnectionState.Connected -> return

                // A reconnect is already in flight; wait for it rather than fighting it with a
                // second attempt, which the SDK would refuse anyway.
                is AssistantClientConnectionState.Connecting -> Unit

                is AssistantClientConnectionState.Disconnected -> {
                    client.connect()
                    return
                }

                // ResumeAvailable is an automatic reconnect hand-off. The SDK has already
                // scheduled the resume attempt, so calling connect() here would cancel that
                // attempt and race it with a fresh session. Wait for the SDK to finish instead.
                is AssistantClientConnectionState.ResumeAvailable -> Unit
            }
        }

        awaitConnectionInFlight()
    }

    /**
     * Waits out an attempt someone else started, and fails if that attempt fails.
     *
     * Waiting for [AssistantClientConnectionState.Connected] alone would wait forever whenever the
     * server is not there: the SDK keeps retrying on a growing backoff, so the state it settles on
     * is `Disconnected`, and a caller parked on `Connected` would sit behind a spinner with nothing
     * said. Failing here costs the caller a retry and tells them why.
     */
    private suspend fun awaitConnectionInFlight() {
        val settledState = connectionState.first { state ->
            state is AssistantClientConnectionState.Connected ||
                state is AssistantClientConnectionState.Disconnected
        }

        if (settledState is AssistantClientConnectionState.Disconnected) {
            throw IllegalStateException(settledState.cause.toShortAgentDriverMessage())
        }
    }
}
