package com.kuts.klaf.room.repositoryImplementations

import com.kuts.klaf.room.databases.KlafRoomDatabase
import com.kuts.klaf.room.databases.ActiveLocalRoomDatabase
import com.kuts.klaf.room.databases.RoomDatabaseSource
import com.kuts.klaf.room.databases.StaticRoomDatabaseSource
import com.kuts.klaf.room.entities.RoomPendingSyncOperation
import com.kuts.klaf.server.contract.SyncOperation
import com.kuts.domain.repositories.IStorageTransactionRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class StorageTransactionRepositoryRoom(
    private val databaseSource: RoomDatabaseSource,
) : IStorageTransactionRepository {

    constructor(roomDatabase: KlafRoomDatabase) : this(StaticRoomDatabaseSource(roomDatabase))

    override suspend fun <R> performWithTransaction(block: suspend () -> R): R {
        return databaseSource.transaction {
            val selection = (databaseSource as? ActiveLocalRoomDatabase)?.selection?.value
            val accountId = selection?.accountEmail ?: return@transaction block()
            databaseSource.requireAccountEditAllowed(accountId)
            val database = selection.database
            val checkpoint = database.syncCheckpointDao().current()?.confirmedRevision ?: 0L
            val conflict = database.syncConflictSnapshotDao().current()
            if (conflict != null && conflict.baseRevision < checkpoint) {
                throw UnresolvedSyncConflictException()
            }
            val beforeDecks = database.deckDao().getAllDecks()
            val beforeCards = database.cardDao().getAllCards()
            val result = block()
            val planned = planSyncOperations(
                beforeDecks = beforeDecks,
                beforeCards = beforeCards,
                afterDecks = database.deckDao().getAllDecks(),
                afterCards = database.cardDao().getAllCards(),
            )
            val alreadyQueuedDeckIds = database.pendingSyncOperationDao().pendingForAccount(accountId)
                .mapNotNull { row ->
                    (Json.decodeFromString<SyncOperation>(row.operationJson) as? SyncOperation.AddDeck)?.deck?.syncId
                }.toSet()
            planned.filterNot { operation ->
                operation is SyncOperation.AddDeck && operation.deck.syncId in alreadyQueuedDeckIds
            }.forEach { operation ->
                database.pendingSyncOperationDao().insert(RoomPendingSyncOperation(
                    accountId = accountId,
                    operationId = operation.operationId,
                    baseRevision = checkpoint,
                    operationJson = Json.encodeToString<SyncOperation>(operation),
                ))
            }
            result
        }
    }
}
