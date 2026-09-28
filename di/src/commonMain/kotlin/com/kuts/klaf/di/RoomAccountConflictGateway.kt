package com.kuts.klaf.di

import com.kuts.domain.common.ConflictResolutionAction
import com.kuts.domain.common.ConflictResolutionDecision
import com.kuts.klaf.deckList.common.AccountSyncOutcome
import com.kuts.klaf.deckList.conflictResolution.AccountConflictGateway
import com.kuts.klaf.deckList.conflictResolution.AccountConflictSnapshot
import com.kuts.klaf.deckList.conflictResolution.ConflictDestination
import com.kuts.klaf.room.databases.ActiveLocalRoomDatabase
import com.kuts.klaf.room.repositoryImplementations.ManualRoomSyncCoordinator
import com.kuts.klaf.room.repositoryImplementations.ManualSyncResult
import com.kuts.klaf.room.repositoryImplementations.RoomSyncConflictStore
import com.kuts.klaf.server.contract.SyncOperation

internal class RoomAccountConflictGateway(
    private val databaseSource: ActiveLocalRoomDatabase,
    private val conflictStore: RoomSyncConflictStore,
    private val coordinator: ManualRoomSyncCoordinator,
) : AccountConflictGateway {

    override suspend fun current(): AccountConflictSnapshot? {
        val account = requireNotNull(databaseSource.selection.value.accountEmail) {
            "Conflict resolution requires a selected account"
        }
        val response = conflictStore.current(account) ?: return null
        val conflict = response.conflicts.singleOrNull()
        val move = (conflict?.localOperation as? SyncOperation.MoveCard)
            ?.takeIf { conflict.reason == "REVIEWED_DECK" }
        val destinations = if (move == null) emptyList() else conflictStore.unreviewedDestinations(
            account,
            setOf(move.sourceDeckSyncId, move.targetDeckSyncId),
        ).map { (syncId, name) -> ConflictDestination(syncId, name) }
        val (deckNames, cardNames) = conflictStore.displayNames(account)
        return AccountConflictSnapshot(account, response, destinations, deckNames, cardNames)
    }

    override suspend fun resolve(accountEmail: String, action: ConflictResolutionAction): AccountSyncOutcome {
        check(databaseSource.selection.value.accountEmail == accountEmail) { "Selected account changed" }
        return when (coordinator.resolve(action)) {
            is ManualSyncResult.Applied -> AccountSyncOutcome.APPLIED
            is ManualSyncResult.NeedsResolution -> AccountSyncOutcome.NEEDS_RESOLUTION
        }
    }

    override suspend fun resolveSelected(
        accountEmail: String,
        decisions: List<ConflictResolutionDecision>,
    ): AccountSyncOutcome {
        check(databaseSource.selection.value.accountEmail == accountEmail) { "Selected account changed" }
        return when (coordinator.resolveSelected(decisions)) {
            is ManualSyncResult.Applied -> AccountSyncOutcome.APPLIED
            is ManualSyncResult.NeedsResolution -> AccountSyncOutcome.NEEDS_RESOLUTION
        }
    }

    override suspend fun resolveMovedCard(
        accountEmail: String,
        targetDeckSyncId: String?,
        newDeckName: String?,
    ): AccountSyncOutcome {
        check(databaseSource.selection.value.accountEmail == accountEmail) { "Selected account changed" }
        return when (coordinator.resolveMoveIntoDeck(targetDeckSyncId, newDeckName)) {
            is ManualSyncResult.Applied -> AccountSyncOutcome.APPLIED
            is ManualSyncResult.NeedsResolution -> AccountSyncOutcome.NEEDS_RESOLUTION
        }
    }
}
