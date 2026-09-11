package com.kuts.klaf.networking.agentDriver

import com.lib.lokdroid.core.logD
import com.lib.lokdroid.core.logE
import kotlinx.coroutines.CancellationException
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
import kotlin.time.TimeSource

/**
 * The app's connection to the assistant.
 *
 * Open only while the assistant is switched on: [switchOn] and [switchOff] follow the provider
 * switch, and nothing else opens the connection. Everything the app asks the assistant -- word
 * insights, mnemonic associations, mnemonic illustrations -- goes through [generateText] and
 * [generateImage] and shares this one connection.
 *
 * The client-side Agent Driver SDK reconnects by itself after ordinary network drops and resumes
 * pending one-shot requests. On Android, the app lifecycle closes an idle connection before the
 * process is frozen and opens a fresh one when the app returns.
 */
class AgentDriverSession(
    serverHost: String,
    clientToken: String,
    private val runtimeDiagnostics: AgentDriverRuntimeDiagnostics = AgentDriverRuntimeDiagnostics.None,
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
                        "Agent Driver SDK request send failed: " +
                            "requestId=${event.requestId.diagnosticValue()}, " +
                            "messageType=${event.messageType}, " +
                            "failure=${event.failure.describeForLog()}; " +
                            runtimeDiagnostics.snapshot(),
                    )

                is KtorAssistantClientEvent.MonitorConnectionFailed ->
                    logE(
                        "Agent Driver SDK connection monitor failed: " +
                            "failure=${event.failure.describeForLog()}; " +
                            runtimeDiagnostics.snapshot(),
                    )

                is KtorAssistantClientEvent.PendingRequestsFailed ->
                    logE(
                        "Agent Driver SDK pending requests failed: " +
                            "requestIds=${event.requestIds.joinToString { it.value }}, " +
                            "failure=${event.failure.describeForLog()}; " +
                            runtimeDiagnostics.snapshot(),
                    )

                is KtorAssistantClientEvent.ReconnectAttemptStarted ->
                    logD(
                        "Agent Driver SDK reconnect starting: attempt=${event.attempt}, " +
                            "resuming=${event.resuming}; ${runtimeDiagnostics.snapshot()}",
                    )

                is KtorAssistantClientEvent.RequestSendStarted ->
                    logD(
                        "Agent Driver SDK request send started: " +
                            "requestId=${event.requestId.diagnosticValue()}, messageType=${event.messageType}",
                    )

                is KtorAssistantClientEvent.RequestSendSucceeded ->
                    logD(
                        "Agent Driver SDK request send succeeded: " +
                            "requestId=${event.requestId.diagnosticValue()}, messageType=${event.messageType}",
                    )

                is KtorAssistantClientEvent.RequestResponseReceived ->
                    logD(
                        "Agent Driver SDK response received: " +
                            "requestId=${event.requestId.value}, messageType=${event.messageType}",
                    )
            }
        },
    )

    val connectionState: StateFlow<AssistantClientConnectionState> get() = client.connectionState

    /** Where this client connects. Fixed for its lifetime, and part of what a session log states. */
    val endpoint: AssistantClientEndpoint get() = client.endpoint

    private val connectMutex = Mutex()
    private val requestLifecycle = AgentDriverRequestLifecycle()
    private var isSwitchedOn = false
    private var nextRequestOperationId = 0L

    /** Opens the connection, and lets requests open it again after a drop the SDK cannot recover. */
    suspend fun switchOn() {
        connectMutex.withLock {
            logD("Agent Driver switch on requested: connection=${connectionState.value.diagnosticName()}")
            isSwitchedOn = true
            awaitOpenSessionLocked()
            logD("Agent Driver switch on completed: connection=${connectionState.value.diagnosticName()}")
        }
    }

    /** Closes the connection. Nothing opens it again until [switchOn]. */
    suspend fun switchOff() {
        connectMutex.withLock {
            logD(
                "Agent Driver switch off requested: connection=${connectionState.value.diagnosticName()}, " +
                    requestLifecycle.diagnosticDescription(),
            )
            isSwitchedOn = false
            client.disconnect()
            logD("Agent Driver switch off completed: connection=${connectionState.value.diagnosticName()}")
        }
    }

    suspend fun generateText(request: TextGenerationRequest): String {
        return withActiveRequest(requestType = "text") {
            client.generateText(request = request)
        }
    }

    suspend fun generateImage(request: ImageGenerationRequest): ByteArray {
        return withActiveRequest(requestType = "image") {
            client.generateImage(request = request).bytes
        }
    }

    /** Closes an idle socket before Android can freeze it in an apparently connected state. */
    internal suspend fun onApplicationBackgrounded() {
        connectMutex.withLock {
            requestLifecycle.onApplicationBackgrounded()
            logD(
                "Agent Driver session handling app background: " +
                    "connection=${connectionState.value.diagnosticName()}, " +
                    requestLifecycle.diagnosticDescription(),
            )
            val disconnected = disconnectIfBackgroundIdleLocked()
            logD(
                "Agent Driver session handled app background: idleDisconnect=$disconnected, " +
                    "connection=${connectionState.value.diagnosticName()}",
            )
        }
    }

    /** Opens a fresh session after an idle background period instead of reusing a stale socket. */
    internal suspend fun onApplicationForegrounded() {
        connectMutex.withLock {
            val shouldRefreshConnection = requestLifecycle.onApplicationForegrounded()
            logD(
                "Agent Driver session handling app foreground: switchedOn=$isSwitchedOn, " +
                    "shouldRefresh=$shouldRefreshConnection, " +
                    "connection=${connectionState.value.diagnosticName()}, " +
                    requestLifecycle.diagnosticDescription(),
            )
            if (!isSwitchedOn || !shouldRefreshConnection) return@withLock

            if (client.connectionState.value !is AssistantClientConnectionState.Disconnected) {
                client.disconnect()
            }
            client.connect()
            logD("Agent Driver foreground refresh completed: connection=${connectionState.value.diagnosticName()}")
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

    private suspend fun <T> withActiveRequest(
        requestType: String,
        block: suspend () -> T,
    ): T {
        val startedAt = TimeSource.Monotonic.markNow()
        val operationId = connectMutex.withLock {
            val allocatedOperationId = nextRequestOperationId++
            requestLifecycle.onRequestStarted()
            try {
                awaitOpenSessionLocked()
                logD(
                    "Agent Driver $requestType request starting: operationId=$allocatedOperationId, " +
                        "connection=${connectionState.value.diagnosticName()}, " +
                        "${requestLifecycle.diagnosticDescription()}; ${runtimeDiagnostics.snapshot()}",
                )
            } catch (throwable: Throwable) {
                requestLifecycle.onRequestFinished()
                logE(
                    "Agent Driver $requestType request could not start: operationId=$allocatedOperationId, " +
                        throwable.describeAgentDriverFailureForLog(),
                )
                throw throwable
            }
            allocatedOperationId
        }

        return try {
            block().also {
                logD(
                    "Agent Driver $requestType request completed successfully: operationId=$operationId, " +
                        "elapsedMs=${startedAt.elapsedNow().inWholeMilliseconds}",
                )
            }
        } catch (cancellation: CancellationException) {
            logD(
                "Agent Driver $requestType request cancelled: operationId=$operationId, " +
                    "elapsedMs=${startedAt.elapsedNow().inWholeMilliseconds}",
            )
            throw cancellation
        } catch (failure: Throwable) {
            logE(
                "Agent Driver $requestType request failed: operationId=$operationId, " +
                    "elapsedMs=${startedAt.elapsedNow().inWholeMilliseconds}, " +
                    "failure=${failure.describeAgentDriverFailureForLog()}; ${runtimeDiagnostics.snapshot()}",
            )
            throw failure
        } finally {
            withContext(NonCancellable) {
                connectMutex.withLock {
                    requestLifecycle.onRequestFinished()
                    val disconnected = disconnectIfBackgroundIdleLocked()
                    logD(
                        "Agent Driver $requestType request cleanup completed: operationId=$operationId, " +
                        "idleDisconnect=$disconnected, " +
                            "connection=${connectionState.value.diagnosticName()}, " +
                            requestLifecycle.diagnosticDescription(),
                    )
                }
            }
        }
    }

    private suspend fun disconnectIfBackgroundIdleLocked(): Boolean {
        val shouldDisconnect = requestLifecycle.shouldDisconnectIdleSession(
            isSwitchedOn = isSwitchedOn,
        )
        if (!shouldDisconnect || client.connectionState.value is AssistantClientConnectionState.Disconnected) {
            return false
        }

        logD(
            "Agent Driver closing idle background session: " +
                "connection=${connectionState.value.diagnosticName()}, " +
                requestLifecycle.diagnosticDescription(),
        )
        client.disconnect()
        return true
    }
}

private fun AssistantClientConnectionState.diagnosticName(): String {
    return this::class.simpleName ?: "Unknown"
}

private fun org.agentdriver.project.domain.model.RequestId?.diagnosticValue(): String {
    return this?.value ?: "none"
}
