package com.kuts.klaf.networking.klafServer

import com.kuts.domain.entities.KlafServerConnectionState
import com.kuts.domain.managers.AccountFailure
import com.kuts.domain.managers.AccountOperationException
import com.kuts.klaf.server.contract.AudioUploadFrameCodec
import com.kuts.klaf.server.contract.ClientSessionEndRequest
import com.kuts.klaf.server.contract.requestClientSessionId
import com.kuts.klaf.server.contract.KLAF_SERVER_WEBSOCKET_PATH
import com.kuts.klaf.server.contract.KlafServerCancelRequest
import com.kuts.klaf.server.contract.KlafServerClientMessage
import com.kuts.klaf.server.contract.KlafServerErrorCode
import com.kuts.klaf.server.contract.KlafServerErrorMessage
import com.kuts.klaf.server.contract.KlafServerMessage
import com.kuts.klaf.server.contract.KlafServerReadyMessage
import com.kuts.klaf.server.contract.MnemonicAssociationGeneratedMessage
import com.kuts.klaf.server.contract.MnemonicImageGeneratedMessage
import com.kuts.klaf.server.contract.PushTokenRegisteredMessage
import com.kuts.klaf.server.contract.SpeechToTextSegmentDto
import com.kuts.klaf.server.contract.VocabularySourceAnalyzedMessage
import com.kuts.klaf.server.contract.VocabularySourceTranscribeCompleteRequest
import com.kuts.klaf.server.contract.VocabularySourceTranscribeRecognitionProgressMessage
import com.kuts.klaf.server.contract.VocabularySourceTranscribeStartRequest
import com.kuts.klaf.server.contract.VocabularySourceTranscribeUploadProgressMessage
import com.kuts.klaf.server.contract.VocabularySourceTranscribedMessage
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
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
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
private const val AUDIO_UPLOAD_WINDOW_BYTES = 2 * 1024 * 1024L
private val CLIENT_SESSION_ID_PATTERN = Regex("[0-9a-f]{1,16}")

sealed interface VocabularySourceTranscriptionSessionEvent {
    data class UploadProgress(val uploadedBytes: Long, val totalBytes: Long) : VocabularySourceTranscriptionSessionEvent
    data class RecognitionProgress(val completedChunks: Int, val totalChunks: Int) : VocabularySourceTranscriptionSessionEvent
    data class Completed(
        val transcript: String,
        val segments: List<SpeechToTextSegmentDto>,
        val serverNotificationSent: Boolean = false,
    ) : VocabularySourceTranscriptionSessionEvent
}

private data class PendingTranscriptionRequest(
    val requestId: String,
    val uploadSession: DefaultClientWebSocketSession,
    val channel: Channel<VocabularySourceTranscriptionSessionEvent>,
    val acknowledgements: Channel<Long> = Channel(capacity = Channel.CONFLATED),
    @Volatile var isUploadCompleted: Boolean = false,
)

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

interface IKlafServerSession : com.kuts.domain.managers.IClientSessionScope {
    val connectionState: StateFlow<KlafServerConnectionState>
    override val clientSessionId: String

    suspend fun connect()

    suspend fun request(message: KlafServerClientMessage): KlafServerMessage

    fun transcribeAudio(
        requestId: String,
        sourceId: Int?,
        sourceTitle: String? = null,
        fileName: String,
        audioFormat: String,
        declaredByteSize: Long,
        audioStreamProvider: suspend (sendChunk: suspend (ByteArray) -> Unit) -> Unit,
    ): Flow<VocabularySourceTranscriptionSessionEvent>

    suspend fun cancelRequest(requestId: String)

    suspend fun nextRequestId(prefix: String): String

    suspend fun disconnect()

    suspend fun endUserSession() = disconnect()
}

class KlafServerSession(
    private val host: String,
    private val port: Int,
    private val isSecure: Boolean,
    private val httpClient: HttpClient,
    private val authenticatedRequestSigner: KlafAuthenticatedRequestSigner? = null,
    private val selectedAccountEmail: Flow<String?>? = null,
) : IKlafServerSession {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
        classDiscriminator = "type"
    }
    private val scope = CoroutineScope(SupervisorJob())
    private val lifecycleMutex = Mutex()
    private val connectionMutex = Mutex()
    private val requestMutex = Mutex()
    private val resendMutex = Mutex()
    private val pendingRequests = mutableMapOf<String, PendingKlafServerRequest>()
    private val pendingTranscriptionRequests = mutableMapOf<String, PendingTranscriptionRequest>()
    private val mutableConnectionState = MutableStateFlow<KlafServerConnectionState>(
        value = KlafServerConnectionState.Disconnected,
    )

    private var session: DefaultClientWebSocketSession? = null
    private var readerJob: Job? = null
    private var reconnectJob: Job? = null
    private var connectionWatchJob: Job? = null
    private var readySignal: CompletableDeferred<Unit>? = null
    @Volatile
    override var clientSessionId = Random.nextLong().toULong().toString(radix = 16)
        private set
    private var nextRequestSequence = 0L
    private var manualDisconnectRequested = false

    init {
        require((authenticatedRequestSigner == null) == (selectedAccountEmail == null)) {
            "WebSocket signer and selected account flow must be configured together."
        }
    }

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
        val originatingSession = clientSessionId
        requireCurrentSessionRequest(message.requestId)
        val activeSession = connectForUserSession(originatingSession)
        val response = CompletableDeferred<KlafServerMessage>()
        val requestStartedAt = TimeSource.Monotonic.markNow()
        requestMutex.withLock {
            if (originatingSession != clientSessionId) throw CancellationException("User session changed")
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
            if (originatingSession != clientSessionId) throw CancellationException("User session changed")
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
            if (originatingSession != clientSessionId) throw CancellationException("User session changed")
            logE(
                "Klaf Server request failed: requestId=${message.requestId}, " +
                    "type=${message::class.simpleName}, elapsed=${requestStartedAt.elapsedNow().asLogValue()}, " +
                    "failure=$throwable",
            )
            throw throwable
        }
    }

    override fun transcribeAudio(
        requestId: String,
        sourceId: Int?,
        sourceTitle: String?,
        fileName: String,
        audioFormat: String,
        declaredByteSize: Long,
        audioStreamProvider: suspend (sendChunk: suspend (ByteArray) -> Unit) -> Unit,
    ): Flow<VocabularySourceTranscriptionSessionEvent> = flow {
        val originatingSession = clientSessionId
        requireCurrentSessionRequest(requestId)
        val activeSession = connectForUserSession(originatingSession)
        val eventChannel = Channel<VocabularySourceTranscriptionSessionEvent>(capacity = Channel.BUFFERED)
        val acknowledgements = Channel<Long>(capacity = Channel.CONFLATED)
        val pending = PendingTranscriptionRequest(
            requestId = requestId,
            uploadSession = activeSession,
            channel = eventChannel,
            acknowledgements = acknowledgements,
        )
        requestMutex.withLock {
            if (originatingSession != clientSessionId) throw CancellationException("User session changed")
            pendingTranscriptionRequests[requestId] = pending
        }

        var uploadJob: Job? = null
        try {
            val startRequest = VocabularySourceTranscribeStartRequest(
                requestId = requestId,
                sourceId = sourceId,
                sourceTitle = sourceTitle,
                fileName = fileName,
                audioFormat = audioFormat,
                declaredByteSize = declaredByteSize,
                clientSessionId = originatingSession,
            )
            sendRequest(session = activeSession, message = startRequest)

            uploadJob = scope.launch {
                try {
                    awaitAcknowledgement(acknowledgements, targetBytes = 0L)
                    var bytesSent = 0L
                    var windowSent = 0L
                    audioStreamProvider { chunk ->
                        if (chunk.isNotEmpty()) {
                            require(chunk.size.toLong() <= declaredByteSize - bytesSent) {
                                "Audio file exceeds its declared byte size."
                            }
                            val encoded = AudioUploadFrameCodec.encode(requestId = requestId, audioChunk = chunk)
                            activeSession.send(Frame.Binary(fin = true, data = encoded))
                            bytesSent += chunk.size
                            windowSent += chunk.size
                            if (windowSent >= AUDIO_UPLOAD_WINDOW_BYTES) {
                                awaitAcknowledgement(
                                    acknowledgements = acknowledgements,
                                    targetBytes = bytesSent,
                                )
                                windowSent = 0L
                            }
                        }
                    }
                    require(bytesSent == declaredByteSize) { "Audio file size changed during upload." }
                    if (bytesSent > 0L) {
                        awaitAcknowledgement(
                            acknowledgements = acknowledgements,
                            targetBytes = bytesSent,
                        )
                    }
                    logD("Audio upload complete; sending complete request: requestId=$requestId, bytesSent=$bytesSent")
                    val completeRequest = VocabularySourceTranscribeCompleteRequest(requestId = requestId)
                    requestMutex.withLock {
                        pending.isUploadCompleted = true
                    }
                    try {
                        sendRequest(session = activeSession, message = completeRequest)
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (failure: Throwable) {
                        logE(
                            "Klaf Server failed to send initial transcribe complete request; " +
                                "will be sent on reconnect: requestId=$requestId, failure=$failure",
                        )
                        scheduleReconnect(reason = failure)
                    }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (failure: Throwable) {
                    logE("Klaf Server audio upload streaming failed: requestId=$requestId, failure=$failure")
                    eventChannel.close(failure)
                }
            }

            for (event in eventChannel) {
                if (originatingSession != clientSessionId) throw CancellationException("User session changed")
                emit(event)
                if (event is VocabularySourceTranscriptionSessionEvent.Completed) {
                    break
                }
            }
            uploadJob.join()
        } catch (cancellation: CancellationException) {
            uploadJob?.cancel()
            withContext(NonCancellable) {
                cancelRequest(requestId = requestId)
            }
            throw cancellation
        } catch (failure: Throwable) {
            if (originatingSession != clientSessionId) throw CancellationException("User session changed")
            throw failure
        } finally {
            requestMutex.withLock {
                pendingTranscriptionRequests.remove(requestId)
            }
            uploadJob?.cancel()
            acknowledgements.close()
            eventChannel.close()
        }
    }

    private suspend fun awaitAcknowledgement(
        acknowledgements: Channel<Long>,
        targetBytes: Long,
    ) {
        withTimeout(30_000) {
            while (acknowledgements.receive() < targetBytes) {
                // Progress can be conflated; the most recent acknowledgement is sufficient.
            }
        }
    }

    override suspend fun cancelRequest(requestId: String) {
        val cancelMessage = KlafServerCancelRequest(
            requestId = nextRequestId(prefix = "request-cancel"),
            targetRequestId = requestId,
        )
        try {
            val activeSession = lifecycleMutex.withLock { session } ?: return
            sendRequest(session = activeSession, message = cancelMessage)
            logD(
                "Klaf Server cancel request sent: " +
                    "requestId=${cancelMessage.requestId}, targetRequestId=$requestId",
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            logE(
                "Klaf Server cancel request failed: " +
                    "requestId=${cancelMessage.requestId}, targetRequestId=$requestId, failure=$throwable",
            )
        }
    }

    override suspend fun nextRequestId(prefix: String): String = requestMutex.withLock {
        "$prefix-$clientSessionId-${nextRequestSequence++}"
    }

    override suspend fun endUserSession() {
        val previousSession = requestMutex.withLock {
            clientSessionId.also {
                clientSessionId = Random.nextLong().toULong().toString(radix = 16)
            }
        }
        // Never connect or queue a logout: only signal an already-open connection.
        withTimeoutOrNull(500L) {
            runCatching {
                val active = lifecycleMutex.withLock { session } ?: return@runCatching
                sendRequest(active, ClientSessionEndRequest("session-end-$previousSession-0", previousSession))
            }
        }
        disconnect()
        // Reconnect only after the account selection has published its new credentials.
    }

    private fun requireCurrentSessionRequest(requestId: String) {
        val originatingSession = requestClientSessionId(requestId)
        if (originatingSession.matches(CLIENT_SESSION_ID_PATTERN) && originatingSession != clientSessionId) {
            throw CancellationException("Request belongs to an ended user session")
        }
    }

    private suspend fun connectForUserSession(originatingSession: String): DefaultClientWebSocketSession = try {
        ensureConnected().also {
            if (originatingSession != clientSessionId) throw CancellationException("User session changed")
        }
    } catch (failure: Throwable) {
        if (originatingSession != clientSessionId) throw CancellationException("User session changed")
        throw failure
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
                pendingTranscriptionRequests.values.forEach { pendingTranscription ->
                    pendingTranscription.acknowledgements.close(IllegalStateException("Klaf Server connection closed."))
                    pendingTranscription.channel.close(
                        IllegalStateException("Klaf Server connection closed."),
                    )
                }
                pendingTranscriptionRequests.clear()
            }
        }

        runCatching { oldSession?.close() }
        runCatching { oldReaderJob?.cancelAndJoin() }
        runCatching { oldReconnectJob?.cancelAndJoin() }
        runCatching { oldConnectionWatchJob?.cancelAndJoin() }
    }

    private suspend fun ensureConnected(connectionAttempt: Int = 1): DefaultClientWebSocketSession = connectionMutex.withLock {
        val existingSession = lifecycleMutex.withLock { session }
        if (existingSession != null) return@withLock existingSession

        val normalizedHost = host.trim()
        require(normalizedHost.isNotBlank()) { "Klaf Server host must not be blank." }

        lifecycleMutex.withLock { manualDisconnectRequested = false }
        logD(
            "Klaf Server connection opening: host=$normalizedHost, port=$port, secure=$isSecure, " +
                "attempt=$connectionAttempt",
        )
        val scheme = if (isSecure) "wss" else "ws"
        val httpScheme = if (isSecure) "https" else "http"
        val newReadySignal = CompletableDeferred<Unit>()
        mutableConnectionState.value = KlafServerConnectionState.Reconnecting(attempt = connectionAttempt)

        var openedSession: DefaultClientWebSocketSession? = null
        try {
            val proof = authenticatedRequestSigner?.let { signer ->
                val email = selectedAccountEmail?.first()?.takeIf(String::isNotBlank)
                    ?: throw IllegalStateException("Sign in to use Klaf Server AI features.")
                KlafServerWebSocketAuthorizer("$httpScheme://$normalizedHost:$port", httpClient, signer)
                    .authorizationHeaders(email)
            }
            val newSession = httpClient.webSocketSession(
                urlString = "$scheme://$normalizedHost:$port$KLAF_SERVER_WEBSOCKET_PATH",
                block = {
                    proof?.let {
                        headers.append("Authorization", it.authorization)
                        headers.append("DPoP", it.dpop)
                    }
                },
            )
            openedSession = newSession
            lifecycleMutex.withLock {
                check(!manualDisconnectRequested) { "Klaf Server connection was disconnected." }
                session = newSession
                readySignal = newReadySignal
                readerJob = scope.launch { readMessages(session = newSession, connectionReadySignal = newReadySignal) }
                connectionWatchJob?.cancel()
                connectionWatchJob = scope.launch {
                    watchConnection(session = newSession, connectionAttempt = connectionAttempt)
                }
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
            withContext(NonCancellable) {
                val failedJobs = lifecycleMutex.withLock {
                    if (!manualDisconnectRequested) {
                        mutableConnectionState.value = KlafServerConnectionState.Error(
                            message = "Klaf Server connection failed.",
                        )
                    }
                    if (session === openedSession) {
                        session = null
                        readySignal = null
                        listOfNotNull(readerJob, connectionWatchJob).also {
                            readerJob = null
                            connectionWatchJob = null
                        }
                    } else {
                        emptyList()
                    }
                }
                failedJobs.forEach { it.cancel() }
                runCatching { openedSession?.close() }
                failedJobs.forEach { it.join() }
            }
            throw throwable
        }
    }

    private suspend fun readMessages(
        session: DefaultClientWebSocketSession,
        connectionReadySignal: CompletableDeferred<Unit>,
    ) {
        var readerFailure: Throwable? = null
        try {
            for (frame in session.incoming) {
                if (frame !is Frame.Text) continue

                val message = decodeServerMessage(text = frame.readText()) ?: continue
                when (message) {
                    is KlafServerReadyMessage -> {
                        logD("Klaf Server ready: protocol=${message.protocolVersion}")
                        lifecycleMutex.withLock {
                            if (this.session === session) mutableConnectionState.value = KlafServerConnectionState.Ready
                        }
                        connectionReadySignal.complete(Unit)
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
                    is VocabularySourceTranscribeUploadProgressMessage -> {
                        val pending = requestMutex.withLock { pendingTranscriptionRequests[message.requestId] }
                        pending?.channel?.trySend(
                            VocabularySourceTranscriptionSessionEvent.UploadProgress(
                                uploadedBytes = message.uploadedBytes,
                                totalBytes = message.totalBytes,
                            ),
                        )
                        pending?.acknowledgements?.trySend(message.uploadedBytes)
                    }
                    is VocabularySourceTranscribeRecognitionProgressMessage -> {
                        val channel = requestMutex.withLock { pendingTranscriptionRequests[message.requestId]?.channel }
                        channel?.trySend(
                            VocabularySourceTranscriptionSessionEvent.RecognitionProgress(
                                completedChunks = message.completedChunks,
                                totalChunks = message.totalChunks,
                            ),
                        )
                    }
                    is VocabularySourceTranscribedMessage -> {
                        val channel = requestMutex.withLock { pendingTranscriptionRequests[message.requestId]?.channel }
                        channel?.send(
                            VocabularySourceTranscriptionSessionEvent.Completed(
                                transcript = message.transcript,
                                segments = message.segments,
                                serverNotificationSent = message.serverNotificationSent,
                            ),
                        )
                    }
                    is PushTokenRegisteredMessage -> completePendingResponse(
                        requestId = message.requestId,
                        message = message,
                    )
                    is KlafServerErrorMessage -> {
                        val requestId = message.requestId
                        if (requestId == null) {
                            logE("Klaf Server connection error: code=${message.code}, message=${message.message}")
                        } else {
                            val pending = requestMutex.withLock {
                                pendingTranscriptionRequests[requestId]
                            }
                            if (pending != null) {
                                val error = IllegalArgumentException("Klaf Server error: code=${message.code}, message=${message.message}")
                                pending.acknowledgements.close(error)
                                pending.channel.close(error)
                            } else {
                                completePendingResponse(
                                    requestId = requestId,
                                    message = message,
                                )
                            }
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
            lifecycleMutex.withLock {
                if (this.session === session) {
                    mutableConnectionState.value = KlafServerConnectionState.Error(message = "Klaf Server reader failed.")
                }
            }
            connectionReadySignal.completeExceptionally(throwable)
        } finally {
            withContext(NonCancellable) {
                connectionReadySignal.completeExceptionally(
                    readerFailure ?: IllegalStateException("Klaf Server connection closed before ready."),
                )
                val pendingTranscriptions = requestMutex.withLock {
                    pendingTranscriptionRequests.values.filter { it.uploadSession === session }
                }
                pendingTranscriptions.forEach { pending ->
                    if (!pending.isUploadCompleted) {
                        val error = readerFailure ?: IllegalStateException("Klaf Server connection closed during audio upload.")
                        pending.acknowledgements.close(error)
                        pending.channel.close(error)
                    } else {
                        logD(
                            "Klaf Server connection closed during transcription recognition; " +
                                "keeping request alive for reconnect: requestId=${pending.requestId}",
                        )
                    }
                }
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
                    if (this@KlafServerSession.session === session) {
                        this@KlafServerSession.session = null
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
                if (throwable is AccountOperationException && throwable.failure == AccountFailure.SIGN_IN_REQUIRED) {
                    failPendingAfterAuthorizationLoss(throwable)
                    return
                }
                logE(
                    "Klaf Server reconnect attempt failed: " +
                        "attempt=$attempt, pendingRequests=${pendingRequestCount()}, failure=$throwable",
                )
                attempt++
                delayMillis = (delayMillis * 2).coerceAtMost(RECONNECT_MAX_DELAY_MILLIS)
            }
        }
    }

    private suspend fun failPendingAfterAuthorizationLoss(failure: AccountOperationException) {
        lifecycleMutex.withLock {
            manualDisconnectRequested = true
            mutableConnectionState.value = KlafServerConnectionState.Error(
                message = "Klaf Server AI authorization is no longer valid.",
            )
        }
        requestMutex.withLock {
            pendingRequests.values.forEach { it.response.completeExceptionally(failure) }
            pendingRequests.clear()
            pendingTranscriptionRequests.values.forEach {
                it.acknowledgements.close(failure)
                it.channel.close(failure)
            }
            pendingTranscriptionRequests.clear()
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

    private suspend fun resendPendingRequests(session: DefaultClientWebSocketSession) = resendMutex.withLock {
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

        val completedTranscriptions = requestMutex.withLock {
            pendingTranscriptionRequests.values.filter { it.isUploadCompleted }.toList()
        }

        completedTranscriptions.forEach { pending ->
            runCatching {
                sendRequest(
                    session = session,
                    message = VocabularySourceTranscribeCompleteRequest(requestId = pending.requestId),
                )
            }.onSuccess {
                logD(
                    "Klaf Server pending transcribe complete request resent: " +
                        "requestId=${pending.requestId}",
                )
            }.onFailure { failure ->
                logE(
                    "Klaf Server pending transcribe complete request resend failed: " +
                        "requestId=${pending.requestId}, failure=$failure",
                )
                throw failure
            }
        }
    }

    private suspend fun pendingRequestCount(): Int = requestMutex.withLock {
        pendingRequests.size + pendingTranscriptionRequests.size
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
