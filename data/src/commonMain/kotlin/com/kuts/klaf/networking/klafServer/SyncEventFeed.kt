package com.kuts.klaf.networking.klafServer

import com.kuts.klaf.server.contract.SyncEventDevice
import com.kuts.klaf.server.contract.SyncEventMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

private const val DEFAULT_RETRY_DELAY_MILLIS = 1_000L
private const val DEFAULT_INITIAL_STATE_TIMEOUT_MILLIS = 5_000L

enum class SyncEventChannelState { GUEST, CONNECTING, CONNECTED, DISCONNECTED }

data class SyncEventFeedStatus(
    val accountEmail: String? = null,
    val channel: SyncEventChannelState = SyncEventChannelState.GUEST,
    val serverRevision: Long? = null,
    val devices: List<SyncEventDevice> = emptyList(),
)

interface SyncEventConnection {

    suspend fun receive(): SyncEventMessage?

    suspend fun close()
}

fun interface SyncEventConnector {

    suspend fun open(email: String, deviceId: String): SyncEventConnection
}

/** Observes server state only; REST synchronization remains a separate manual action. */
class SyncEventFeed(
    private val selectedAccountEmail: Flow<String?>,
    private val deviceIdProvider: suspend () -> String,
    private val connector: SyncEventConnector,
    private val scope: CoroutineScope,
    private val retryDelayMillis: Long = DEFAULT_RETRY_DELAY_MILLIS,
    private val initialStateTimeoutMillis: Long = DEFAULT_INITIAL_STATE_TIMEOUT_MILLIS,
) {

    constructor(
        selectedAccountEmail: Flow<String?>,
        deviceId: String,
        connector: SyncEventConnector,
        scope: CoroutineScope,
        retryDelayMillis: Long = DEFAULT_RETRY_DELAY_MILLIS,
        initialStateTimeoutMillis: Long = DEFAULT_INITIAL_STATE_TIMEOUT_MILLIS,
    ) : this(selectedAccountEmail, { deviceId }, connector, scope, retryDelayMillis, initialStateTimeoutMillis) {
        require(deviceId.isNotBlank()) { "Sync event device ID is required" }
    }

    private val mutableState = MutableStateFlow(SyncEventFeedStatus())
    private var worker: Job? = null

    val state: StateFlow<SyncEventFeedStatus> = mutableState.asStateFlow()

    init {
        require(retryDelayMillis > 0L) { "Sync event retry delay must be positive" }
        require(initialStateTimeoutMillis > 0L) { "Sync event initial state timeout must be positive" }
    }

    fun start() {
        if (worker?.isActive == true) return
        worker = scope.launch {
            selectedAccountEmail.distinctUntilChanged().collectLatest { email ->
                if (email == null) {
                    mutableState.value = SyncEventFeedStatus()
                } else {
                    observeAccount(email)
                }
            }
        }
    }

    suspend fun stop() {
        worker?.cancelAndJoin()
        worker = null
        mutableState.value = SyncEventFeedStatus()
    }

    private suspend fun observeAccount(email: String) {
        mutableState.value = SyncEventFeedStatus(email, SyncEventChannelState.CONNECTING)
        while (currentCoroutineContext().isActive) {
            mutableState.value = mutableState.value.copy(channel = SyncEventChannelState.CONNECTING)
            try {
                val deviceId = deviceIdProvider().also {
                    require(it.isNotBlank()) { "Sync event device ID is required" }
                }
                val connection = connector.open(email, deviceId)
                try {
                    val initial = withTimeoutOrNull(initialStateTimeoutMillis) { connection.receive() }
                    check(initial is SyncEventMessage.State) { "Sync event initial state is missing" }
                    mutableState.value = SyncEventFeedStatus(
                        accountEmail = email,
                        channel = SyncEventChannelState.CONNECTED,
                        serverRevision = initial.revision,
                        devices = initial.devices,
                    )
                    while (true) {
                        when (val message = connection.receive() ?: break) {
                            is SyncEventMessage.State -> {
                                val previousRevision = mutableState.value.serverRevision ?: 0L
                                mutableState.value = SyncEventFeedStatus(
                                    accountEmail = email,
                                    channel = SyncEventChannelState.CONNECTED,
                                    serverRevision = maxOf(previousRevision, message.revision),
                                    devices = message.devices,
                                )
                            }
                            is SyncEventMessage.RevisionChanged -> {
                                val current = mutableState.value
                                mutableState.value = current.copy(
                                    serverRevision = maxOf(current.serverRevision ?: 0L, message.revision),
                                )
                            }
                        }
                    }
                } finally {
                    withContext(NonCancellable) { connection.close() }
                }
            } catch (_: TimeoutCancellationException) {
                // A connector timeout is retryable; cancellation of this observer is not.
                currentCoroutineContext().ensureActive()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                // The indicator shows unavailability; a later attempt refreshes server state.
            }
            mutableState.value = mutableState.value.copy(channel = SyncEventChannelState.DISCONNECTED)
            delay(retryDelayMillis)
        }
    }
}
