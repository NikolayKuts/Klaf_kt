package com.kuts.klaf.networking.agentDriver

import com.lib.lokdroid.core.logD
import com.lib.lokdroid.core.logE
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.agentdriver.project.ktorclient.external.AssistantClientConnectionState
import org.agentdriver.project.ktorclient.external.AssistantClientEndpoint
import org.agentdriver.project.ktorclient.external.ClientAuthentication
import org.agentdriver.project.ktorclient.external.IAssistantClient
import org.agentdriver.project.ktorclient.external.KtorAssistantClient
import org.agentdriver.project.ktorclient.external.KtorAssistantClientEvent
import org.agentdriver.project.ktorclient.external.KtorAssistantClientEventListener
import org.agentdriver.project.protocol.ImageGenerationRequest
import org.agentdriver.project.protocol.TextGenerationRequest

/**
 * The app's connection to the assistant.
 *
 * Open only while the assistant is switched on: [switchOn] and [switchOff] follow the provider
 * switch, and nothing else opens the connection. Everything the app asks the assistant -- word
 * insights, mnemonic associations, mnemonic illustrations -- goes through [generateText] and
 * [generateImage] and shares this one connection.
 *
 * The SDK reconnects by itself after ordinary network drops. On Android, the app lifecycle closes
 * an idle connection before the process is frozen and opens a fresh one when the app returns.
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
        // The AgentDriver server issued this and checks it itself, so the tunnel in front only
        // has to carry the connection rather than decide who may open one.
        authentication = ClientAuthentication.BearerToken(token = clientToken.trim()),
        eventListener = KtorAssistantClientEventListener { event ->
            when (event) {
                is KtorAssistantClientEvent.RequestSendFailed ->
                    logE(
                        "Agent Driver SDK failed to send ${event.messageType}: " +
                            event.failure.describeForLog(),
                    )

                is KtorAssistantClientEvent.MonitorConnectionFailed ->
                    logE(
                        "Agent Driver SDK connection monitor failed: " +
                            event.failure.describeForLog(),
                    )

                else -> logD("Agent Driver SDK event: $event")
            }
        },
    )

    val connectionState: StateFlow<AssistantClientConnectionState> get() = client.connectionState

    /** Where this client connects. Fixed for its lifetime, and part of what a session log states. */
    val endpoint: AssistantClientEndpoint get() = client.endpoint

    private val connectMutex = Mutex()
    private val requestLifecycle = AgentDriverRequestLifecycle()
    private var isSwitchedOn = false

    /** Opens the connection, and lets requests open it again after a drop the SDK cannot recover. */
    suspend fun switchOn() {
        connectMutex.withLock {
            isSwitchedOn = true
            awaitOpenSessionLocked()
        }
    }

    /** Closes the connection. Nothing opens it again until [switchOn]. */
    suspend fun switchOff() {
        connectMutex.withLock {
            isSwitchedOn = false
            client.disconnect()
        }
    }

    suspend fun generateText(request: TextGenerationRequest): String {
        return withActiveRequest {
            client.generateText(request = request)
        }
    }

    suspend fun generateImage(request: ImageGenerationRequest): ByteArray {
        return withActiveRequest {
            client.generateImage(request = request).bytes
        }
    }

    /** Closes an idle socket before Android can freeze it in an apparently connected state. */
    internal suspend fun onApplicationBackgrounded() {
        connectMutex.withLock {
            requestLifecycle.onApplicationBackgrounded()
            disconnectIfBackgroundIdleLocked()
        }
    }

    /** Opens a fresh session after an idle background period instead of reusing a stale socket. */
    internal suspend fun onApplicationForegrounded() {
        connectMutex.withLock {
            val shouldRefreshConnection = requestLifecycle.onApplicationForegrounded()
            if (!isSwitchedOn || !shouldRefreshConnection) return

            if (client.connectionState.value !is AssistantClientConnectionState.Disconnected) {
                client.disconnect()
            }
            client.connect()
        }
    }

    /**
     * Waits for an open connection, opening it if this is the first request.
     *
     * This must run under [connectMutex]. The same lock protects lifecycle connection changes and
     * request registration, so Android cannot disconnect the socket between this check and send.
     */
    private suspend fun awaitOpenSessionLocked() {
        check(value = isSwitchedOn) { "The assistant is switched off." }

        when (connectionState.value) {
            is AssistantClientConnectionState.Connected -> return

            // A reconnect is already in flight; wait for it rather than fighting it with a
            // second attempt, which the SDK would refuse anyway.
            is AssistantClientConnectionState.Connecting -> Unit

            is AssistantClientConnectionState.Disconnected -> {
                client.connect()
                return
            }

            // ResumeAvailable is an automatic reconnect hand-off. The SDK has already scheduled
            // the resume attempt, so calling connect() here would race it with a fresh session.
            is AssistantClientConnectionState.ResumeAvailable -> Unit
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

    private suspend fun <T> withActiveRequest(block: suspend () -> T): T {
        connectMutex.withLock {
            requestLifecycle.onRequestStarted()
            try {
                awaitOpenSessionLocked()
            } catch (throwable: Throwable) {
                requestLifecycle.onRequestFinished()
                throw throwable
            }
        }

        return try {
            block()
        } finally {
            withContext(NonCancellable) {
                connectMutex.withLock {
                    requestLifecycle.onRequestFinished()
                    disconnectIfBackgroundIdleLocked()
                }
            }
        }
    }

    private suspend fun disconnectIfBackgroundIdleLocked() {
        val shouldDisconnect = requestLifecycle.shouldDisconnectIdleSession(
            isSwitchedOn = isSwitchedOn,
        )
        if (
            shouldDisconnect &&
            client.connectionState.value !is AssistantClientConnectionState.Disconnected
        ) {
            client.disconnect()
        }
    }
}
