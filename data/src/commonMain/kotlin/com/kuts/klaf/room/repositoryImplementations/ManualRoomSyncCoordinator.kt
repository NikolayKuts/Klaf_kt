package com.kuts.klaf.room.repositoryImplementations

import com.kuts.domain.common.ConflictResolutionAction
import com.kuts.domain.common.ConflictResolutionDecision
import com.kuts.domain.entities.Deck
import com.kuts.klaf.room.databases.ActiveLocalRoomDatabase
import com.kuts.klaf.server.contract.SyncBootstrapResponse
import com.kuts.klaf.server.contract.SyncOperation
import com.kuts.klaf.server.contract.SyncRequest
import com.kuts.klaf.server.contract.SyncResponse
import com.kuts.klaf.server.contract.accountInterimDeckSyncId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

sealed interface ManualSyncResult {

    data class Applied(val revision: Long) : ManualSyncResult

    /** The server may already have committed independent operations; preserve IDs for a safe retry. */
    data class NeedsResolution(val response: SyncResponse) : ManualSyncResult
}

/** Invoked only by an explicit user sync action; it never starts a background request. */
class ManualRoomSyncCoordinator(
    private val databaseSource: ActiveLocalRoomDatabase,
    private val outbox: RoomSyncOutbox,
    private val applier: RoomSyncDeltaApplier,
    private val deviceIdProvider: suspend () -> String,
    private val sendRequest: suspend (SyncRequest) -> SyncResponse,
    private val confirmAppliedRevision: suspend (String, String, Long) -> Unit,
    private val conflictStore: RoomSyncConflictStore = RoomSyncConflictStore(databaseSource),
    private val resolver: RoomSyncConflictResolver = RoomSyncConflictResolver(databaseSource, outbox, applier),
    private val partialApplier: RoomSyncPartialApplier = RoomSyncPartialApplier(databaseSource, outbox, applier),
    private val fetchBootstrap: (suspend (String, String) -> SyncBootstrapResponse)? = null,
    private val prepareImages: suspend (SyncRequest) -> Unit = {},
    private val downloadImages: suspend (String) -> Unit = {},
) {

    constructor(
        databaseSource: ActiveLocalRoomDatabase,
        outbox: RoomSyncOutbox,
        applier: RoomSyncDeltaApplier,
        deviceId: String,
        sendRequest: suspend (SyncRequest) -> SyncResponse,
        confirmAppliedRevision: suspend (String, String, Long) -> Unit,
        conflictStore: RoomSyncConflictStore = RoomSyncConflictStore(databaseSource),
        resolver: RoomSyncConflictResolver = RoomSyncConflictResolver(databaseSource, outbox, applier),
        partialApplier: RoomSyncPartialApplier = RoomSyncPartialApplier(databaseSource, outbox, applier),
        fetchBootstrap: (suspend (String, String) -> SyncBootstrapResponse)? = null,
        prepareImages: suspend (SyncRequest) -> Unit = {},
        downloadImages: suspend (String) -> Unit = {},
    ) : this(databaseSource, outbox, applier, { deviceId }, sendRequest, confirmAppliedRevision,
        conflictStore, resolver, partialApplier, fetchBootstrap, prepareImages, downloadImages) {
        require(deviceId.isNotBlank()) { "Device ID is required" }
    }

    private val syncMutex = Mutex()
    private val mutableAttemptState = MutableStateFlow<ManualSyncAttemptState>(ManualSyncAttemptState.Idle)

    val attemptState: StateFlow<ManualSyncAttemptState> = mutableAttemptState.asStateFlow()

    suspend fun synchronize(): ManualSyncResult = runAttempt { accountId ->
        synchronizeSelected(accountId)
    }

    suspend fun resolve(action: ConflictResolutionAction): ManualSyncResult = when (action) {
        ConflictResolutionAction.ACCEPT_SERVER -> resolveAllWithServer()
        ConflictResolutionAction.KEEP_LOCAL_DECK -> resolveLocalDeckEdits()
        ConflictResolutionAction.KEEP_LOCAL_CARD -> resolveLocalCardEdits()
        ConflictResolutionAction.KEEP_LOCAL_SOURCE -> runAttempt { accountId ->
            resolver.keepLocalSources(accountId)
            synchronizeSelected(accountId)
        }
        ConflictResolutionAction.RESCUE_MOVED_CARD -> resolveMovedCardRescue()
        ConflictResolutionAction.RESTORE_DELETED_DECK -> resolveDeletedDeckWithLocalCardEdits()
        ConflictResolutionAction.KEEP_REMOVAL_RETAIN_SCHEDULE -> resolveRemovalAfterReview(false)
        ConflictResolutionAction.KEEP_REMOVAL_DUE_NOW -> resolveRemovalAfterReview(true)
        ConflictResolutionAction.RETARGET_MOVED_CARD -> error("Choose a destination deck before resolving the move")
    }

    suspend fun resolveSelected(decisions: List<ConflictResolutionDecision>): ManualSyncResult = runAttempt { accountId ->
        resolver.resolveSelected(accountId, decisions)
        synchronizeSelected(accountId)
    }

    suspend fun resolveAllWithServer(): ManualSyncResult = runAttempt { accountId ->
        resolver.acceptServerForAll(accountId)
        synchronizeSelected(accountId)
    }

    suspend fun resolveLocalDeckEdits(): ManualSyncResult = runAttempt { accountId ->
        resolver.keepLocalDeckEdits(accountId)
        synchronizeSelected(accountId)
    }

    suspend fun resolveLocalCardEdits(): ManualSyncResult = runAttempt { accountId ->
        resolver.keepLocalCardEdits(accountId)
        synchronizeSelected(accountId)
    }

    suspend fun resolveMovedCardRescue(): ManualSyncResult = runAttempt { accountId ->
        resolver.rescueMovedCardsAfterSourceDeletion(accountId)
        synchronizeSelected(accountId)
    }

    suspend fun resolveDeletedDeckWithLocalCardEdits(): ManualSyncResult = runAttempt { accountId ->
        resolver.restoreDeletedDeckWithLocalCardEdits(accountId)
        synchronizeSelected(accountId)
    }

    suspend fun resolveRemovalAfterReview(makeDueNow: Boolean): ManualSyncResult = runAttempt { accountId ->
        resolver.keepCardRemovalAfterReview(accountId, makeDueNow)
        synchronizeSelected(accountId)
    }

    suspend fun resolveMoveIntoDeck(
        targetDeckSyncId: String? = null,
        newDeckName: String? = null,
    ): ManualSyncResult = runAttempt { accountId ->
        resolver.retargetMoveIntoUnreviewedDeck(accountId, targetDeckSyncId, newDeckName)
        synchronizeSelected(accountId)
    }

    private suspend fun runAttempt(action: suspend (String) -> ManualSyncResult): ManualSyncResult = syncMutex.withLock {
        val accountId = selectedAccount()
        databaseSource.beginManualSyncAttempt(accountId)
        mutableAttemptState.value = ManualSyncAttemptState.Running(accountId)
        try {
            action(accountId).also { mutableAttemptState.value = ManualSyncAttemptState.Idle }
        } catch (cancellation: CancellationException) {
            mutableAttemptState.value = ManualSyncAttemptState.Idle
            throw cancellation
        } catch (failure: Exception) {
            mutableAttemptState.value = ManualSyncAttemptState.Failed(accountId)
            throw failure
        } finally {
            withContext(NonCancellable) { databaseSource.endManualSyncAttempt(accountId) }
        }
    }

    private fun selectedAccount(): String = requireNotNull(databaseSource.selection.value.accountEmail) {
        "Manual synchronization requires a signed-in account"
    }

    private suspend fun synchronizeSelected(accountId: String): ManualSyncResult {
        check(databaseSource.selection.value.accountEmail == accountId) { "Selected account changed" }
        val deviceId = deviceIdProvider().also { require(it.isNotBlank()) { "Device ID is required" } }
        outbox.prepareVocabularyUpload(accountId)
        val revision = outbox.confirmedRevision(accountId)
        val pending = outbox.pendingForAccount(accountId)
        val savedConflict = conflictStore.current(accountId)
        if (savedConflict != null && savedConflict.revision == revision &&
            savedConflict.delta.fromRevision < revision
        ) {
            return ManualSyncResult.NeedsResolution(savedConflict)
        }
        require(pending.all { it.baseRevision == revision }) {
            "Pending changes require conflict resolution before synchronization"
        }
        val localDecks = databaseSource.current().deckDao().getAllDecks()
        val interimSyncId = accountInterimDeckSyncId(accountId)
        val onlyEmptyInterim = localDecks.size == 1 &&
            localDecks.single().id == Deck.INTERIM_DECK_ID &&
            localDecks.single().syncId == interimSyncId &&
            localDecks.single().cardQuantity == 0
        val onlyInterimPending = pending.isEmpty() || pending.size == 1 &&
            (pending.single().operation as? SyncOperation.AddDeck)?.deck?.syncId == interimSyncId
        val safeForInitialSnapshot = (localDecks.isEmpty() && pending.isEmpty() ||
            onlyEmptyInterim && onlyInterimPending) && databaseSource.current().cardDao().getAllCards().isEmpty()
        val hasNoCheckpoint = databaseSource.current().syncCheckpointDao().current() == null
        if (savedConflict == null && fetchBootstrap != null && (hasNoCheckpoint || safeForInitialSnapshot)) {
            val snapshot = fetchBootstrap.invoke(accountId, deviceId)
            check(databaseSource.selection.value.accountEmail == accountId) {
                "Selected account changed during synchronization"
            }
            if (snapshot.decks.isNotEmpty() || snapshot.cards.isNotEmpty() || snapshot.sources.isNotEmpty() || snapshot.ignoredWords.isNotEmpty()) {
                if (safeForInitialSnapshot) {
                    applier.applyInitialSnapshot(accountId, snapshot, pending.map { it.operation.operationId })
                    downloadImages(accountId)
                    confirmAppliedRevision(accountId, deviceId, snapshot.revision)
                    return ManualSyncResult.Applied(snapshot.revision)
                }
                check(!hasNoCheckpoint || snapshot.revision > 0L) {
                    "Cannot apply initial server snapshot while unrelated local changes are pending"
                }
            }
        }
        val request = SyncRequest(
            email = accountId,
            deviceId = deviceId,
            protocolVersion = 3,
            baseRevision = revision,
            operations = pending.map(PendingSyncOperation::operation),
            includeVocabularySnapshot = databaseSource.current().syncCheckpointDao().current()?.vocabularySyncInitialized != true,
        )
        prepareImages(request)
        val response = sendRequest(request)
        check(databaseSource.selection.value.accountEmail == accountId) {
            "Selected account changed during synchronization"
        }
        require(response.delta.fromRevision == request.baseRevision) {
            "Server delta does not begin at the requested revision"
        }
        val requestIds = request.operations.map { it.operationId }.toSet()
        val conflictIds = response.conflicts.map { it.operationId }
        require(conflictIds.distinct().size == conflictIds.size) { "Duplicate conflict operation ID" }
        require(response.acceptedOperationIds.intersect(conflictIds.toSet()).isEmpty()) {
            "Server both accepted and conflicted an operation"
        }
        require(response.acceptedOperationIds + conflictIds == requestIds) {
            "Server did not account for every submitted operation"
        }
        if (response.conflicts.isNotEmpty()) {
            if (!partialApplier.tryApply(accountId, pending, response)) {
                conflictStore.record(accountId, requestIds, response)
            }
            return ManualSyncResult.NeedsResolution(response)
        }
        applier.applyConflictFree(accountId, response)
        downloadImages(accountId)
        confirmAppliedRevision(accountId, deviceId, response.revision)
        return ManualSyncResult.Applied(response.revision)
    }
}
