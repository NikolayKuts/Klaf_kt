package com.kuts.klaf.room.repositoryImplementations

import com.kuts.klaf.room.databases.ActiveLocalRoomDatabase
import com.kuts.klaf.room.entities.RoomSyncConflictSnapshot
import com.kuts.klaf.server.contract.SyncOperation
import com.kuts.klaf.server.contract.SyncResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Applies independent source changes or provably disjoint deck additions while preserving conflicts. */
class RoomSyncPartialApplier(
    private val databaseSource: ActiveLocalRoomDatabase,
    private val outbox: RoomSyncOutbox,
    private val applier: RoomSyncDeltaApplier,
) {

    private val json = Json { classDiscriminator = "type" }

    suspend fun tryApply(
        accountId: String,
        pending: List<PendingSyncOperation>,
        response: SyncResponse,
    ): Boolean {
        if (response.conflicts.isEmpty()) return false
        if (response.conflicts.all {
            it.localOperation is SyncOperation.UpsertVocabularySource || it.localOperation is SyncOperation.DeleteVocabularySource
        }) return applySourceConflictDelta(accountId, pending, response)
        if (response.acceptedOperationIds.isEmpty()) return false
        if (response.delta.cards.isNotEmpty() || response.delta.deletedDeckSyncIds.isNotEmpty() ||
            response.delta.deletedCardSyncIds.isNotEmpty()
        ) return false
        val accepted = pending.filter { it.operation.operationId in response.acceptedOperationIds }
            .map(PendingSyncOperation::operation)
        if (accepted.size != response.acceptedOperationIds.size || accepted.any { it !is SyncOperation.AddDeck }) {
            return false
        }
        val conflicted = response.conflicts.map { it.localOperation }
        if (conflicted.any { it !is SyncOperation.EditDeck }) return false
        val acceptedDeckIds = accepted.map { (it as SyncOperation.AddDeck).deck.syncId }.toSet()
        val protectedDeckIds = conflicted.map { (it as SyncOperation.EditDeck).deckSyncId }.toSet()
        if (acceptedDeckIds.size != accepted.size || acceptedDeckIds.any(protectedDeckIds::contains)) return false
        val deltaDeckIds = response.delta.decks.map { it.syncId }.toSet()
        if (!deltaDeckIds.containsAll(acceptedDeckIds) ||
            !((acceptedDeckIds + protectedDeckIds).containsAll(deltaDeckIds))
        ) return false

        outbox.applyAccepted(accountId, response.acceptedOperationIds.toList(), response.revision) {
            val database = databaseSource.current()
            val checkpoint = database.syncCheckpointDao().current()?.confirmedRevision ?: 0L
            require(response.delta.fromRevision == checkpoint) { "Partial delta has a stale checkpoint" }
            require(response.delta.toRevision == response.revision) { "Response and delta revisions differ" }
            val storedPending = database.pendingSyncOperationDao().pendingForAccount(accountId)
            require(storedPending.map { it.operationId }.toSet() == pending.map { it.operation.operationId }.toSet()) {
                "Pending operations changed while the server request was in flight"
            }
            require(storedPending.all { it.baseRevision == checkpoint }) {
                "Pending operation has a different base revision"
            }
            applier.applyRows(database, response.delta.copy(
                decks = response.delta.decks.filter { it.syncId in acceptedDeckIds },
            ))
            database.syncConflictSnapshotDao().save(RoomSyncConflictSnapshot(
                accountId = accountId,
                baseRevision = checkpoint,
                responseJson = json.encodeToString(response),
            ))
        }
        return true
    }

    private suspend fun applySourceConflictDelta(
        accountId: String, pending: List<PendingSyncOperation>, response: SyncResponse,
    ): Boolean {
        val conflictIds = response.conflicts.map { it.operationId }.toSet()
        require(response.acceptedOperationIds.intersect(conflictIds).isEmpty()) { "Operation is both accepted and conflicted" }
        require(response.acceptedOperationIds + conflictIds == pending.map { it.operation.operationId }.toSet()) {
            "Source response does not account for every pending operation"
        }
        val protectedSources = response.conflicts.map {
            when (val operation = it.localOperation) {
                is SyncOperation.UpsertVocabularySource -> operation.source.syncId
                is SyncOperation.DeleteVocabularySource -> operation.sourceSyncId
                else -> error("Not a source conflict")
            }
        }.toSet()
        outbox.applyAccepted(accountId, response.acceptedOperationIds.toList(), response.revision) {
            val database = databaseSource.current()
            val checkpoint = database.syncCheckpointDao().current()?.confirmedRevision ?: 0L
            require(response.delta.fromRevision == checkpoint && response.delta.toRevision == response.revision) {
                "Source partial delta has a stale checkpoint"
            }
            val stored = database.pendingSyncOperationDao().pendingForAccount(accountId)
            require(stored.map { it.operationId }.toSet() == pending.map { it.operation.operationId }.toSet() &&
                stored.all { it.baseRevision == checkpoint }) { "Pending source operations changed in flight" }
            applier.applyRows(database, response.delta.copy(
                sources = response.delta.sources.filter { it.syncId !in protectedSources },
                deletedSourceSyncIds = response.delta.deletedSourceSyncIds.filter { it !in protectedSources },
            ))
            database.syncConflictSnapshotDao().save(RoomSyncConflictSnapshot(accountId, checkpoint,
                json.encodeToString(response)))
        }
        return true
    }
}
