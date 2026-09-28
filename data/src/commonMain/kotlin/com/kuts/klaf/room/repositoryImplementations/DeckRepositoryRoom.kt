package com.kuts.klaf.room.repositoryImplementations

import com.kuts.domain.common.simplifiedItemMap
import com.kuts.domain.entities.Deck
import com.kuts.domain.repositories.IDeckRepository
import com.kuts.klaf.room.databases.ActiveLocalRoomDatabase
import com.kuts.klaf.room.databases.KlafRoomDatabase
import com.kuts.klaf.room.databases.RoomDatabaseSource
import com.kuts.klaf.room.databases.StaticRoomDatabaseSource
import com.kuts.klaf.room.entities.RoomDeck
import com.kuts.klaf.room.entities.RoomPendingSyncOperation
import com.kuts.klaf.room.newSyncId
import com.kuts.klaf.room.toDomainEntity
import com.kuts.klaf.room.toRoomEntity
import com.kuts.klaf.server.contract.SyncOperation
import com.kuts.klaf.server.contract.accountInterimDeckSyncId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@OptIn(ExperimentalCoroutinesApi::class)
class DeckRepositoryRoom(
    private val databaseSource: RoomDatabaseSource,
) : IDeckRepository {

    constructor(roomDatabase: KlafRoomDatabase) : this(StaticRoomDatabaseSource(roomDatabase))

    private val roomDatabase: KlafRoomDatabase
        get() = databaseSource.current()

    override fun fetchDeckSource(): Flow<List<Deck>> {
        return databaseSource.databases.flatMapLatest { database ->
            database.deckDao().getObservableDecks()
                .simplifiedItemMap { roomDeck -> roomDeck.toDomainEntity() }
        }
    }

    override suspend fun fetchAllDecks(): List<Deck> {
        return roomDatabase.deckDao()
            .getAllDecks()
            .map { roomDeck -> roomDeck.toDomainEntity() }
    }

    override fun fetchObservableDeckById(deckId: Int): Flow<Deck?> {
        return databaseSource.databases.flatMapLatest { database ->
            database.deckDao().getObservableDeckById(deckId = deckId)
                .map { roomDeck: RoomDeck? -> roomDeck?.toDomainEntity() }
        }
    }

    override suspend fun insertDeck(deck: Deck): Int {
        val selection = (databaseSource as? ActiveLocalRoomDatabase)?.selection?.value
        val database = selection?.database ?: roomDatabase
        val accountEmail = selection?.accountEmail
        val isNewAccountInterim = accountEmail != null && deck.id == Deck.INTERIM_DECK_ID &&
            database.deckDao().getDeckById(Deck.INTERIM_DECK_ID) == null
        val row = deck.toRoomEntity().let { roomDeck ->
            if (accountEmail != null && deck.id == Deck.INTERIM_DECK_ID) {
                roomDeck.copy(syncId = accountInterimDeckSyncId(accountEmail))
            } else roomDeck
        }
        val deckId = database.deckDao().insertDeck(row).toInt()
        if (isNewAccountInterim) {
            val accountId = requireNotNull(accountEmail)
            val revision = database.syncCheckpointDao().current()?.confirmedRevision ?: 0L
            val operation = SyncOperation.AddDeck("interim-add-${newSyncId()}", row.toInitialSyncDeck())
            database.pendingSyncOperationDao().insert(RoomPendingSyncOperation(
                accountId = accountId,
                operationId = operation.operationId,
                baseRevision = revision,
                operationJson = Json.encodeToString<SyncOperation>(operation),
            ))
        }

        return deck.id.takeIf { it > 0 } ?: deckId
    }

    override suspend fun insertDeckAtPath(
        deck: Deck,
        rootEmailPath: String
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun removeDeck(deckId: Int) {
        roomDatabase.deckDao().deleteDeck(deckId)
    }

    override suspend fun getDeckById(deckId: Int): Deck? {
        return roomDatabase.deckDao().getDeckById(deckId)?.toDomainEntity()
    }

    override suspend fun getCardQuantityInDeck(deckId: Int): Int {
        return roomDatabase.cardDao().getCardQuantityInDeck(deckId)
    }
}
