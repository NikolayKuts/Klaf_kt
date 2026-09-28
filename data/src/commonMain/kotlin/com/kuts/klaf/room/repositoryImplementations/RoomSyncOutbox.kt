package com.kuts.klaf.room.repositoryImplementations

import com.kuts.klaf.room.databases.ActiveLocalRoomDatabase
import com.kuts.klaf.room.databases.KlafRoomDatabase
import com.kuts.klaf.room.entities.RoomPendingSyncOperation
import com.kuts.klaf.room.entities.RoomSyncCheckpoint
import com.kuts.klaf.server.contract.SyncOperation
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class PendingSyncOperation(
    val baseRevision: Long,
    val operation: SyncOperation,
)

class UnresolvedSyncConflictException : IllegalStateException(
    "Resolve the partially applied synchronization conflict before editing",
)

class RoomSyncOutbox(private val databaseSource: ActiveLocalRoomDatabase) {

    suspend fun confirmedRevision(accountId: String): Long = databaseSource.transaction {
        selectedDatabase(accountId).syncCheckpointDao().current()?.confirmedRevision ?: 0L
    }

    suspend fun <R> recordChange(
        accountId: String,
        baseRevision: Long,
        operation: SyncOperation,
        localChange: suspend () -> R,
    ): R {
        require(baseRevision >= 0L) { "Base revision cannot be negative" }
        require(operation.operationId.isNotBlank()) { "Operation ID is required" }

        return databaseSource.transaction {
            val database = selectedDatabase(accountId)
            databaseSource.requireAccountEditAllowed(accountId)
            val confirmedRevision = database.syncCheckpointDao().current()?.confirmedRevision ?: 0L
            require(baseRevision == confirmedRevision) {
                "Local operation base revision does not match the selected account checkpoint"
            }
            val conflict = database.syncConflictSnapshotDao().current()
            if (conflict != null && conflict.baseRevision < confirmedRevision) {
                throw UnresolvedSyncConflictException()
            }
            val result = localChange()
            database.pendingSyncOperationDao().insert(
                RoomPendingSyncOperation(
                    accountId = accountId,
                    operationId = operation.operationId,
                    baseRevision = baseRevision,
                    operationJson = Json.encodeToString<SyncOperation>(operation),
                ),
            )
            result
        }
    }

    suspend fun pendingForAccount(accountId: String): List<PendingSyncOperation> = databaseSource.transaction {
        selectedDatabase(accountId).pendingSyncOperationDao().pendingForAccount(accountId).map { row ->
            PendingSyncOperation(
                baseRevision = row.baseRevision,
                operation = Json.decodeFromString<SyncOperation>(row.operationJson),
            )
        }
    }

    suspend fun <R> applyAccepted(
        accountId: String,
        operationIds: List<String>,
        newRevision: Long,
        applyServerChanges: suspend () -> R,
    ): R {
        require(newRevision >= 0L) { "Confirmed revision cannot be negative" }
        return databaseSource.transaction {
            val database = selectedDatabase(accountId)
            val checkpoint = database.syncCheckpointDao()
            val previousRevision = checkpoint.current()?.confirmedRevision ?: 0L
            require(newRevision >= previousRevision) { "Confirmed revision cannot move backwards" }
            val result = applyServerChanges()
            checkpoint.save(RoomSyncCheckpoint(confirmedRevision = newRevision))
            operationIds.forEach { operationId ->
                database.pendingSyncOperationDao().removeAccepted(accountId, operationId)
            }
            result
        }
    }

    private fun selectedDatabase(accountId: String): KlafRoomDatabase {
        val selected = databaseSource.selection.value
        require(selected.accountEmail != null && selected.accountEmail == accountId) {
            "Outbox account does not match the selected database"
        }
        return selected.database
    }
}
