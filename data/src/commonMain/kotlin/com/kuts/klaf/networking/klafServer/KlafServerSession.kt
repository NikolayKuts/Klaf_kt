package com.kuts.klaf.networking.klafServer

import com.kuts.domain.entities.KlafServerConnectionState
import com.kuts.klaf.server.contract.KLAF_SERVER_WEBSOCKET_PATH
import com.kuts.klaf.server.contract.KlafServerClientMessage
import com.kuts.klaf.server.contract.KlafServerErrorMessage
import com.kuts.klaf.server.contract.KlafServerMessage
import com.kuts.klaf.server.contract.KlafServerReadyMessage
import com.kuts.klaf.server.contract.MnemonicAssociationGeneratedMessage
import com.kuts.klaf.server.contract.MnemonicImageGeneratedMessage
import com.kuts.klaf.server.contract.VocabularySourceAnalyzedMessage
import com.kuts.klaf.server.contract.WordInsightsGeneratedMessage
import com.lib.lokdroid.core.logD
import com.lib.lokdroid.core.logE
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.TimeSource

private const val READY_TIMEOUT_MILLIS = 5_000L
private const val RECONNECT_INITIAL_DELAY_MILLIS = 1_000L
private const val RECONNECT_MAX_DELAY_MILLIS = 10_000L
private const val CONNECTION_WATCH_INTERVAL_MILLIS = 15_000L

private data class PendingKlafServerRequest(
    val message: KlafServerClientMessage,
    val response: CompletableDeferred<KlafServerMessage>,
)

private data class ConnectionWatchSnapshot(
    val state: KlafServerConnectionState,
    val readerActive: Boolean,
    val reconnectActive: Boolean,
    val manualDisconnect: Boolean,
)

interface IKlafServerSession {
    val connectionState: StateFlow<KlafServerConnectionState>

    suspend fun connect()

    suspend fun request(message: KlafServerClientMessage): KlafServerMessage

    suspend fun nextRequestId(prefix: String): String

    suspend fun disconnect()
}

class KlafServerSession(
    private val host: String,
    private val port: Int,
    private val isSecure: Boolean,
    private val httpClient: HttpClient,
) : IKlafServerSession {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
        classDiscriminator = "type"
    }
    private val scope = CoroutineScope(SupervisorJob())
    private val lifecycleMutex = Mutex()
    private val requestMutex = Mutex()
    private val pendingRequests = mutableMapOf<String, PendingKlafServerRequest>()
    private val mutableConnectionState = MutableStateFlow<KlafServerConnectionState>(
        value = KlafServerConnectionState.Disconnected,
    )

    private var session: DefaultClientWebSocketSession? = null
    private var readerJob: Job? = null
    private var reconnectJob: Job? = null
    private var connectionWatchJob: Job? = null
    private var readySignal: CompletableDeferred<Unit>? = null
    private val requestIdNamespace = Random.nextLong().toString()
    private var nextRequestSequence = 0L
    private var manualDisconnectRequested = false

    override val connectionState: StateFlow<KlafServerConnectionState> = mutableConnectionState.asStateFlow()

    override suspend fun connect() {
        val oldReconnectJob = lifecycleMutex.withLock {
            manualDisconnectRequested = false
            reconnectJob
        }
        if (oldReconnectJob?.isActive == true) {
            logD(
                "Klaf Server immediate connect cancelling scheduled reconnect: " +
                    "state=${connectionState.value}, pendingRequests=${pendingRequestCount()}",
            )
        }
        oldReconnectJob?.cancelAndJoin()
        lifecycleMutex.withLock {
            if (reconnectJob === oldReconnectJob) {
                reconnectJob = null
            }
        }
        logD(
            "Klaf Server immediate connect requested: " +
                "state=${connectionState.value}, pendingRequests=${pendingRequestCount()}",
        )
        val activeSession = ensureConnected()
        resendPendingRequests(session = activeSession)
    }

    override suspend fun request(message: KlafServerClientMessage): KlafServerMessage {
        val activeSession = ensureConnected()
        val response = CompletableDeferred<KlafServerMessage>()
        val requestStartedAt = TimeSource.Monotonic.markNow()
        requestMutex.withLock {
            pendingRequests[message.requestId] = PendingKlafServerRequest(
                message = message,
                response = response,
            )
        }

        try {
            try {
                sendRequest(
                    session = activeSession,
                    message = message,
                )
            } catch (sendCancellation: CancellationException) {
                throw sendCancellation
            } catch (sendFailure: Throwable) {
                logE(
                    "Klaf Server request send failed; waiting for reconnect: " +
                        "requestId=${message.requestId}, type=${message::class.simpleName}, " +
                        "pendingRequests=${pendingRequestCount()}, failure=$sendFailure",
                )
                scheduleReconnect(reason = sendFailure)
            }
            val serverMessage = response.await()
            logRequestCompleted(
                request = message,
                response = serverMessage,
                elapsed = requestStartedAt.elapsedNow(),
            )
            return serverMessage
        } catch (cancellation: CancellationException) {
            requestMutex.withLock {
                pendingRequests.remove(message.requestId)
            }
            logD(
                "Klaf Server request cancelled: requestId=${message.requestId}, " +
                    "type=${message::class.simpleName}, elapsed=${requestStartedAt.elapsedNow().asLogValue()}",
            )
            throw cancellation
        } catch (throwable: Throwable) {
            requestMutex.withLock {
                pendingRequests.remove(message.requestId)
            }
            logE(
                "Klaf Server request failed: requestId=${message.requestId}, " +
                    "type=${message::class.simpleName}, elapsed=${requestStartedAt.elapsedNow().asLogValue()}, " +
                    "failure=$throwable",
            )
            throw throwable
        }
    }

    override suspend fun nextRequestId(prefix: String): String = requestMutex.withLock {
        "$prefix-$requestIdNamespace-${nextRequestSequence++}"
    }

    override suspend fun disconnect() {
        val oldSession: DefaultClientWebSocketSession?
        val oldReaderJob: Job?
        val oldReconnectJob: Job?
        val oldConnectionWatchJob: Job?

        lifecycleMutex.withLock {
            oldSession = session
            oldReaderJob = readerJob
            oldReconnectJob = reconnectJob
            oldConnectionWatchJob = connectionWatchJob

            session = null
            readerJob = null
            reconnectJob = null
            connectionWatchJob = null
            readySignal = null
            manualDisconnectRequested = true
            mutableConnectionState.value = KlafServerConnectionState.Disconnected

            requestMutex.withLock {
                pendingRequests.values.forEach { pendingRequest ->
                    pendingRequest.response.completeExceptionally(
                        IllegalStateException("Klaf Server connection closed."),
                    )
                }
                pendingRequests.clear()
            }
        }

        runCatching { oldSession?.close() }
        runCatching { oldReaderJob?.cancelAndJoin() }
        runCatching { oldReconnectJob?.cancelAndJoin() }
        runCatching { oldConnectionWatchJob?.cancelAndJoin() }
    }

    private suspend fun ensureConnected(connectionAttempt: Int = 1): DefaultClientWebSocketSession = lifecycleMutex.withLock {
        session?.let { return@withLock it }

        val normalizedHost = host.trim()
        require(normalizedHost.isNotBlank()) { "Klaf Server host must not be blank." }

        manualDisconnectRequested = false
        logD(
            "Klaf Server connection opening: host=$normalizedHost, port=$port, secure=$isSecure, " +
                "attempt=$connectionAttempt",
        )
        val scheme = if (isSecure) "wss" else "ws"
        val newReadySignal = CompletableDeferred<Unit>()
        mutableConnectionState.value = KlafServerConnectionState.Reconnecting(attempt = connectionAttempt)

        try {
            val newSession = httpClient.webSocketSession(
                urlString = "$scheme://$normalizedHost:$port$KLAF_SERVER_WEBSOCKET_PATH",
            )
            session = newSession
            readySignal = newReadySignal
            readerJob = scope.launch {
                readMessages(session = newSession)
            }
            connectionWatchJob?.cancel()
            connectionWatchJob = scope.launch {
                watchConnection(
                    session = newSession,
                    connectionAttempt = connectionAttempt,
                )
            }
            withTimeout(READY_TIMEOUT_MILLIS) {
                newReadySignal.await()
            }
            logD(
                "Klaf Server connection opened and ready: " +
                    "attempt=$connectionAttempt, pendingRequests=${pendingRequestCount()}",
            )
            newSession
        } catch (throwable: Throwable) {
            logE("Klaf Server connection failed: attempt=$connectionAttempt, failure=$throwable")
            mutableConnectionState.value = KlafServerConnectionState.Error(
                message = throwable.message ?: "Klaf Server connection failed.",
            )
            readySignal = null
            val failedSession = session
            val failedReaderJob = readerJob
            val failedConnectionWatchJob = connectionWatchJob
            session = null
            readerJob = null
            connectionWatchJob = null
            runCatching { failedSession?.close() }
            runCatching { failedReaderJob?.cancelAndJoin() }
            runCatching { failedConnectionWatchJob?.cancelAndJoin() }
            throw throwable
        }
    }

    private suspend fun readMessages(session: DefaultClientWebSocketSession) {
        var readerFailure: Throwable? = null
        try {
            for (frame in session.incoming) {
                if (frame !is Frame.Text) continue

                val message = decodeServerMessage(text = frame.readText()) ?: continue
                when (message) {
                    is KlafServerReadyMessage -> {
                        logD("Klaf Server ready: protocol=${message.protocolVersion}")
                        mutableConnectionState.value = KlafServerConnectionState.Ready
                        readySignal?.complete(Unit)
                    }
                    is MnemonicAssociationGeneratedMessage -> completePendingResponse(
                        requestId = message.requestId,
                        message = message,
                    )
                    is MnemonicImageGeneratedMessage -> completePendingResponse(
                        requestId = message.requestId,
                        message = message,
                    )
                    is WordInsightsGeneratedMessage -> completePendingResponse(
                        requestId = message.requestId,
                        message = message,
                    )
                    is VocabularySourceAnalyzedMessage -> completePendingResponse(
                        requestId = message.requestId,
                        message = message,
                    )
                    is KlafServerErrorMessage -> {
                        val requestId = message.requestId
                        if (requestId == null) {
                            logE("Klaf Server connection error: code=${message.code}, message=${message.message}")
                        } else {
                            completePendingResponse(
                                requestId = requestId,
                                message = message,
                            )
                        }
                    }
                }
            }
        } catch (cancellation: CancellationException) {
            readerFailure = cancellation
            throw cancellation
        } catch (throwable: Throwable) {
            readerFailure = throwable
            logE(
                "Klaf Server reader failed: " +
                    "pendingRequests=${pendingRequestCount()}, failure=${throwable.stackTraceToString()}",
            )
            mutableConnectionState.value = KlafServerConnectionState.Error(
                message = throwable.message ?: "Klaf Server reader failed.",
            )
            readySignal?.completeExceptionally(throwable)
        } finally {
            val closeReason = runCatching {
                withTimeoutOrNull(timeMillis = 250L) {
                    session.closeReason.await()
                }
            }.getOrNull()
            val pendingCount = pendingRequestCount()
            var shouldReconnect = false
            var manualDisconnect = false
            var stateBeforeClose: KlafServerConnectionState

            lifecycleMutex.withLock {
                stateBeforeClose = mutableConnectionState.value
                manualDisconnect = manualDisconnectRequested
                if (this.session === session) {
                    this.session = null
                    readerJob = null
                    connectionWatchJob?.cancel()
                    connectionWatchJob = null
                    readySignal = null
                    if (mutableConnectionState.value == KlafServerConnectionState.Ready) {
                        mutableConnectionState.value = KlafServerConnectionState.Disconnected
                    }
                    shouldReconnect = !manualDisconnectRequested
                }
            }
            logD(
                "Klaf Server reader closed: stateBeforeClose=$stateBeforeClose, " +
                    "closeReason=${closeReason ?: "none"}, pendingRequests=$pendingCount, " +
                    "manualDisconnect=$manualDisconnect, shouldReconnect=$shouldReconnect",
            )
            if (shouldReconnect) {
                scheduleReconnect(reason = readerFailure)
            }
        }
    }

    private suspend fun scheduleReconnect(reason: Throwable?) {
        val pendingCount = pendingRequestCount()
        lifecycleMutex.withLock {
            if (manualDisconnectRequested) {
                logD(
                    "Klaf Server reconnect skipped: manualDisconnect=true, " +
                        "reason=${reason?.message ?: reason?.let { it::class.simpleName } ?: "connection closed"}, " +
                        "pendingRequests=$pendingCount",
                )
                return
            }
            if (reconnectJob?.isActive == true) {
                logD(
                    "Klaf Server reconnect skipped: reconnect already active, " +
                        "reason=${reason?.message ?: reason?.let { it::class.simpleName } ?: "connection closed"}, " +
                        "pendingRequests=$pendingCount",
                )
                return
            }

            logD(
                "Klaf Server reconnect scheduled: " +
                    "reason=${reason?.message ?: reason?.let { it::class.simpleName } ?: "connection closed"}, " +
                    "pendingRequests=$pendingCount",
            )
            reconnectJob = scope.launch {
                reconnectUntilReady()
            }
        }
    }

    private suspend fun watchConnection(
        session: DefaultClientWebSocketSession,
        connectionAttempt: Int,
    ) {
        var tick = 0L
        while (true) {
            delay(timeMillis = CONNECTION_WATCH_INTERVAL_MILLIS)

            val pendingCount = pendingRequestCount()
            val snapshot = lifecycleMutex.withLock {
                if (this.session !== session) return

                ConnectionWatchSnapshot(
                    state = mutableConnectionState.value,
                    readerActive = readerJob?.isActive == true,
                    reconnectActive = reconnectJob?.isActive == true,
                    manualDisconnect = manualDisconnectRequested,
                )
            }
            if (pendingCount == 0 && snapshot.state == KlafServerConnectionState.Ready) {
                continue
            }

            tick++
            logD(
                "Klaf Server connection watchdog: tick=$tick, attempt=$connectionAttempt, " +
                    "state=${snapshot.state}, pendingRequests=$pendingCount, " +
                    "readerActive=${snapshot.readerActive}, reconnectActive=${snapshot.reconnectActive}, " +
                    "manualDisconnect=${snapshot.manualDisconnect}",
            )
        }
    }

    private suspend fun reconnectUntilReady() {
        var attempt = 1
        var delayMillis = RECONNECT_INITIAL_DELAY_MILLIS

        while (true) {
            try {
                logD(
                    "Klaf Server reconnect attempt scheduled: " +
                        "attempt=$attempt, delay=${delayMillis}ms, pendingRequests=${pendingRequestCount()}",
                )
                delay(timeMillis = delayMillis)
                val activeSession = ensureConnected(connectionAttempt = attempt)
                resendPendingRequests(session = activeSession)
                logD(
                    "Klaf Server reconnect succeeded: " +
                        "attempt=$attempt, pendingRequests=${pendingRequestCount()}",
                )
                return
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                logE(
                    "Klaf Server reconnect attempt failed: " +
                        "attempt=$attempt, pendingRequests=${pendingRequestCount()}, failure=$throwable",
                )
                attempt++
                delayMillis = (delayMillis * 2).coerceAtMost(RECONNECT_MAX_DELAY_MILLIS)
            }
        }
    }

    private fun decodeServerMessage(text: String): KlafServerMessage? = try {
        json.decodeFromString<KlafServerMessage>(text)
    } catch (exception: SerializationException) {
        logE("Klaf Server message decoding failed: ${exception.message}")
        null
    }

    private suspend fun completePendingResponse(
        requestId: String,
        message: KlafServerMessage,
    ) {
        requestMutex.withLock {
            pendingRequests.remove(requestId)
        }?.response?.complete(message)
    }

    private suspend fun sendRequest(
        session: DefaultClientWebSocketSession,
        message: KlafServerClientMessage,
    ) {
        session.send(json.encodeToString<KlafServerClientMessage>(message))
        logD("Klaf Server request sent: requestId=${message.requestId}, type=${message::class.simpleName}")
    }

    private suspend fun resendPendingRequests(session: DefaultClientWebSocketSession) {
        val pendingRequestsSnapshot = requestMutex.withLock {
            pendingRequests.values.toList()
        }

        pendingRequestsSnapshot.forEach { pendingRequest ->
            runCatching {
                sendRequest(
                    session = session,
                    message = pendingRequest.message,
                )
            }.onSuccess {
                logD(
                    "Klaf Server pending request resent: " +
                        "requestId=${pendingRequest.message.requestId}, " +
                        "type=${pendingRequest.message::class.simpleName}",
                )
            }.onFailure { failure ->
                logE(
                    "Klaf Server pending request resend failed: " +
                        "requestId=${pendingRequest.message.requestId}, " +
                        "type=${pendingRequest.message::class.simpleName}, failure=$failure",
                )
                throw failure
            }
        }
    }

    private suspend fun pendingRequestCount(): Int = requestMutex.withLock {
        pendingRequests.size
    }

    private fun logRequestCompleted(
        request: KlafServerClientMessage,
        response: KlafServerMessage,
        elapsed: Duration,
    ) {
        val baseMessage = "Klaf Server request completed: requestId=${request.requestId}, " +
            "type=${request::class.simpleName}, response=${response::class.simpleName}, " +
            "elapsed=${elapsed.asLogValue()}"

        if (response is KlafServerErrorMessage) {
            logE("$baseMessage, code=${response.code}, message=${response.message}")
        } else {
            logD(baseMessage)
        }
    }
}

private fun Duration.asLogValue(): String {
    val elapsedMillis = inWholeMilliseconds
    val elapsedSeconds = elapsedMillis / 1_000.0
    return "${elapsedMillis}ms (${elapsedSeconds.formatOneDecimal()}s)"
}

private fun Double.formatOneDecimal(): String {
    val rounded = (this * 10).toLong()
    val whole = rounded / 10
    val fraction = rounded % 10
    return "$whole.$fraction"
}
