package com.kuts.klaf.networking.klafServer

import com.kuts.domain.entities.KlafServerConnectionState
import com.kuts.klaf.server.contract.KLAF_SERVER_WEBSOCKET_PATH
import com.kuts.klaf.server.contract.KlafServerClientMessage
import com.kuts.klaf.server.contract.KlafServerErrorMessage
import com.kuts.klaf.server.contract.KlafServerMessage
import com.kuts.klaf.server.contract.KlafServerReadyMessage
import com.kuts.klaf.server.contract.MnemonicAssociationGeneratedMessage
import com.kuts.klaf.server.contract.MnemonicImageGeneratedMessage
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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Duration
import kotlin.time.TimeSource

private const val READY_TIMEOUT_MILLIS = 5_000L

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
    private val pendingResponses = mutableMapOf<String, CompletableDeferred<KlafServerMessage>>()
    private val mutableConnectionState = MutableStateFlow<KlafServerConnectionState>(
        value = KlafServerConnectionState.Disconnected,
    )

    private var session: DefaultClientWebSocketSession? = null
    private var readerJob: kotlinx.coroutines.Job? = null
    private var readySignal: CompletableDeferred<Unit>? = null
    private var nextRequestSequence = 0L

    override val connectionState: StateFlow<KlafServerConnectionState> = mutableConnectionState.asStateFlow()

    override suspend fun connect() {
        ensureConnected()
    }

    override suspend fun request(message: KlafServerClientMessage): KlafServerMessage {
        val activeSession = ensureConnected()
        val response = CompletableDeferred<KlafServerMessage>()
        val requestStartedAt = TimeSource.Monotonic.markNow()
        requestMutex.withLock {
            pendingResponses[message.requestId] = response
        }

        try {
            activeSession.send(json.encodeToString<KlafServerClientMessage>(message))
            logD("Klaf Server request sent: requestId=${message.requestId}, type=${message::class.simpleName}")
            val serverMessage = response.await()
            logRequestCompleted(
                request = message,
                response = serverMessage,
                elapsed = requestStartedAt.elapsedNow(),
            )
            return serverMessage
        } catch (cancellation: CancellationException) {
            requestMutex.withLock {
                pendingResponses.remove(message.requestId)
            }
            logD(
                "Klaf Server request cancelled: requestId=${message.requestId}, " +
                    "type=${message::class.simpleName}, elapsed=${requestStartedAt.elapsedNow().asLogValue()}",
            )
            throw cancellation
        } catch (throwable: Throwable) {
            requestMutex.withLock {
                pendingResponses.remove(message.requestId)
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
        "$prefix-${nextRequestSequence++}"
    }

    override suspend fun disconnect() {
        lifecycleMutex.withLock {
            val oldSession = session
            val oldReaderJob = readerJob
            session = null
            readerJob = null
            readySignal = null
            mutableConnectionState.value = KlafServerConnectionState.Disconnected

            requestMutex.withLock {
                pendingResponses.values.forEach { response ->
                    response.completeExceptionally(IllegalStateException("Klaf Server connection closed."))
                }
                pendingResponses.clear()
            }

            runCatching { oldSession?.close() }
            runCatching { oldReaderJob?.cancelAndJoin() }
        }
    }

    private suspend fun ensureConnected(): DefaultClientWebSocketSession = lifecycleMutex.withLock {
        session?.let { return@withLock it }

        val normalizedHost = host.trim()
        require(normalizedHost.isNotBlank()) { "Klaf Server host must not be blank." }

        logD("Klaf Server connection opening: host=$normalizedHost, port=$port, secure=$isSecure")
        val scheme = if (isSecure) "wss" else "ws"
        val newReadySignal = CompletableDeferred<Unit>()
        mutableConnectionState.value = KlafServerConnectionState.Reconnecting(attempt = 1)

        try {
            val newSession = httpClient.webSocketSession(
                urlString = "$scheme://$normalizedHost:$port$KLAF_SERVER_WEBSOCKET_PATH",
            )
            session = newSession
            readySignal = newReadySignal
            readerJob = scope.launch {
                readMessages(session = newSession)
            }
            withTimeout(READY_TIMEOUT_MILLIS) {
                newReadySignal.await()
            }
            logD("Klaf Server connection opened and ready.")
            newSession
        } catch (throwable: Throwable) {
            logE("Klaf Server connection failed: $throwable")
            mutableConnectionState.value = KlafServerConnectionState.Error(
                message = throwable.message ?: "Klaf Server connection failed.",
            )
            readySignal = null
            val failedSession = session
            val failedReaderJob = readerJob
            session = null
            readerJob = null
            runCatching { failedSession?.close() }
            runCatching { failedReaderJob?.cancelAndJoin() }
            throw throwable
        }
    }

    private suspend fun readMessages(session: DefaultClientWebSocketSession) {
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
        } catch (throwable: Throwable) {
            logE("Klaf Server reader failed: $throwable")
            mutableConnectionState.value = KlafServerConnectionState.Error(
                message = throwable.message ?: "Klaf Server reader failed.",
            )
            readySignal?.completeExceptionally(throwable)
            failPendingResponses(failure = throwable)
        } finally {
            lifecycleMutex.withLock {
                if (this.session === session) {
                    this.session = null
                    readerJob = null
                    readySignal = null
                    if (mutableConnectionState.value == KlafServerConnectionState.Ready) {
                        mutableConnectionState.value = KlafServerConnectionState.Disconnected
                    }
                }
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
            pendingResponses.remove(requestId)
        }?.complete(message)
    }

    private suspend fun failPendingResponses(failure: Throwable) {
        requestMutex.withLock {
            val responses = pendingResponses.values.toList()
            pendingResponses.clear()
            responses
        }.forEach { response ->
            response.completeExceptionally(failure)
        }
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
