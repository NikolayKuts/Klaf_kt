package com.kuts.klaf.room.repositoryImplementations

import com.kuts.klaf.room.databases.ActiveLocalRoomDatabase
import com.kuts.klaf.room.databases.KlafRoomDatabase
import com.kuts.klaf.room.entities.RoomSyncConflictSnapshot
import com.kuts.klaf.server.contract.SyncResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Keeps the latest unresolved response in the selected account's Room file. */
class RoomSyncConflictStore(private val databaseSource: ActiveLocalRoomDatabase) {

    private val json = Json { classDiscriminator = "type" }

    suspend fun record(accountId: String, expectedOperationIds: Set<String>, response: SyncResponse) {
        require(response.conflicts.isNotEmpty()) { "Only conflict responses can be saved" }
        require(response.delta.toRevision == response.revision) { "Response and delta revisions differ" }
        val conflictIds = response.conflicts.map { it.operationId }
        require(conflictIds.distinct().size == conflictIds.size) { "Duplicate conflict operation ID" }
        require(response.acceptedOperationIds.intersect(conflictIds.toSet()).isEmpty()) {
            "Server both accepted and conflicted an operation"
        }
        require(response.acceptedOperationIds + conflictIds == expectedOperationIds) {
            "Server did not account for every submitted operation"
        }

        databaseSource.transaction {
            val database = selectedDatabase(accountId)
            val checkpoint = database.syncCheckpointDao().current()?.confirmedRevision ?: 0L
            require(checkpoint == response.delta.fromRevision) { "Conflict response has a stale base revision" }
            val pendingIds = database.pendingSyncOperationDao().pendingForAccount(accountId)
                .map { it.operationId }.toSet()
            require(pendingIds == expectedOperationIds) { "Pending operations changed during the server request" }
            database.syncConflictSnapshotDao().save(RoomSyncConflictSnapshot(
                accountId = accountId,
                baseRevision = checkpoint,
                responseJson = json.encodeToString(response),
            ))
        }
    }

    suspend fun current(accountId: String): SyncResponse? = databaseSource.transaction {
        val snapshot = selectedDatabase(accountId).syncConflictSnapshotDao().current() ?: return@transaction null
        check(snapshot.accountId == accountId) { "Conflict snapshot belongs to another account" }
        json.decodeFromString<SyncResponse>(snapshot.responseJson)
    }

    suspend fun unreviewedDestinations(accountId: String, excludedSyncIds: Set<String>): List<Pair<String, String>> =
        databaseSource.transaction {
            selectedDatabase(accountId).deckDao().getAllDecks()
                .filter { deck -> deck.repetitionQuantity == 0 && deck.syncId !in excludedSyncIds }
                .map { deck -> deck.syncId to deck.name }
        }

    suspend fun displayNames(accountId: String): Pair<Map<String, String>, Map<String, String>> =
        databaseSource.transaction {
            val database = selectedDatabase(accountId)
            database.deckDao().getAllDecks().associate { it.syncId to it.name } to
                database.cardDao().getAllCards().associate { it.syncId to it.foreignWord }
        }

    private fun selectedDatabase(accountId: String): KlafRoomDatabase {
        val selected = databaseSource.selection.value
        require(selected.accountEmail != null && selected.accountEmail == accountId) {
            "Conflict account does not match the selected database"
        }
        return selected.database
    }
}
