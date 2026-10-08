package com.kuts.klaf.room.repositoryImplementations

import com.kuts.klaf.networking.klafServer.SyncEventChannelState
import com.kuts.klaf.networking.klafServer.SyncEventFeedStatus
import com.kuts.klaf.room.databases.ActiveLocalRoomDatabase
import com.kuts.klaf.server.contract.SyncEventDevice
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import com.kuts.klaf.server.contract.SyncOperation
import com.kuts.klaf.room.identity
import com.kuts.klaf.room.toSyncWord
import kotlinx.serialization.json.Json

enum class SyncIndicatorState { HIDDEN, GREEN, YELLOW, RED, GRAY, SYNCING }

sealed interface ManualSyncAttemptState {

    data object Idle : ManualSyncAttemptState

    data class Running(val accountId: String) : ManualSyncAttemptState

    data class Failed(val accountId: String) : ManualSyncAttemptState
}

data class RoomSyncStatus(
    val accountEmail: String? = null,
    val indicator: SyncIndicatorState = SyncIndicatorState.HIDDEN,
    val confirmedRevision: Long = 0L,
    val serverRevision: Long? = null,
    val pendingOperationCount: Int = 0,
    val hasConflict: Boolean = false,
    val devices: List<SyncEventDevice> = emptyList(),
)

/** Combines durable local sync state with the event channel; it never starts a sync request. */
@OptIn(ExperimentalCoroutinesApi::class)
class RoomSyncStatusObserver(
    private val databaseSource: ActiveLocalRoomDatabase,
    private val events: Flow<SyncEventFeedStatus>,
    private val attempts: Flow<ManualSyncAttemptState> = flowOf(ManualSyncAttemptState.Idle),
    private val hasMissingImages: suspend (String) -> Boolean = { false },
) {

    val status: Flow<RoomSyncStatus> = databaseSource.selection.flatMapLatest { selection ->
        val email = selection.accountEmail
        if (email == null) {
            flowOf(RoomSyncStatus())
        } else {
            val database = selection.database
            combine(
                combine(database.pendingSyncOperationDao().observePendingForAccount(email),
                    database.vocabularySourceDao().getObservableSources(), database.ignoredVocabularyWordDao().observeWords()) { rows, sources, words ->
                    val operations = rows.map { Json.decodeFromString<SyncOperation>(it.operationJson) }
                    val sourceIds = operations.mapNotNull {
                        when (it) {
                            is SyncOperation.UpsertVocabularySource -> it.source.syncId
                            is SyncOperation.DeleteVocabularySource -> it.sourceSyncId
                            else -> null
                        }
                    }.toSet()
                    val wordIds = operations.filterIsInstance<SyncOperation.AddIgnoredVocabularyWord>().map { it.word.identity() }.toSet()
                    rows.size + sources.count { it.lastChangedServerRevision == 0L && it.syncId !in sourceIds } +
                        words.count { it.lastChangedServerRevision == 0L && it.toSyncWord().identity() !in wordIds }
                },
                database.syncCheckpointDao().observeCurrent(),
                database.syncConflictSnapshotDao().observeCurrent(),
                events,
                attempts,
            ) { pendingCount, checkpoint, conflict, event, attempt ->
                val confirmedRevision = checkpoint?.confirmedRevision ?: 0L
                val accountEvent = event.takeIf { it.accountEmail == email }
                val channel = accountEvent?.channel ?: SyncEventChannelState.DISCONNECTED
                val serverRevision = accountEvent?.serverRevision
                val hasConflict = conflict != null && conflict.accountId == email
                val attemptRunning = attempt is ManualSyncAttemptState.Running && attempt.accountId == email
                val attemptFailed = attempt is ManualSyncAttemptState.Failed && attempt.accountId == email
                val indicator = when {
                    attemptRunning -> SyncIndicatorState.SYNCING
                    hasConflict || attemptFailed || conflict != null -> SyncIndicatorState.RED
                    channel == SyncEventChannelState.CONNECTED && serverRevision != null &&
                        serverRevision < confirmedRevision -> SyncIndicatorState.RED
                    channel != SyncEventChannelState.CONNECTED || serverRevision == null -> SyncIndicatorState.GRAY
                    checkpoint == null || !checkpoint.vocabularySyncInitialized || pendingCount > 0 || serverRevision > confirmedRevision ->
                        SyncIndicatorState.YELLOW
                    hasMissingImages(email) -> SyncIndicatorState.YELLOW
                    else -> SyncIndicatorState.GREEN
                }
                RoomSyncStatus(
                    accountEmail = email,
                    indicator = indicator,
                    confirmedRevision = confirmedRevision,
                    serverRevision = serverRevision,
                    pendingOperationCount = pendingCount,
                    hasConflict = hasConflict,
                    devices = accountEvent?.devices.orEmpty().map { device ->
                        if (channel == SyncEventChannelState.CONNECTED) device else device.copy(connected = false)
                    },
                )
            }
        }
    }.distinctUntilChanged()
}
