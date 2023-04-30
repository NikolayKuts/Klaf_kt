package com.example.klaf.data.room.repositoryImplementations

import com.example.domain.common.simplifiedItemMap
import com.example.domain.entities.Deck
import com.example.domain.repositories.DeckRepository
import com.example.klaf.data.room.databases.KlafRoomDatabase
import com.example.klaf.data.room.entities.RoomDeck
import com.example.klaf.data.room.toDomainEntity
import com.example.klaf.data.room.toRoomEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeckRepositoryRoomImp @Inject constructor(
    private val roomDatabase: KlafRoomDatabase,
) : DeckRepository {

    override fun fetchDeckSource(): Flow<List<Deck>> {
        return roomDatabase.deckDao()
            .getObservableDecks()
            .simplifiedItemMap { roomDeck -> roomDeck.toDomainEntity() }
    }

    override suspend fun fetchAllDecks(): List<Deck> {
        return roomDatabase.deckDao()
            .getAllDecks()
            .map { roomDeck -> roomDeck.toDomainEntity() }
    }

    override fun fetchObservableDeckById(deckId: Int): Flow<Deck?> {
        return roomDatabase.deckDao()
            .getObservableDeckById(deckId = deckId)
            .map { roomDeck: RoomDeck? -> roomDeck?.toDomainEntity() }
    }

    override suspend fun insertDeck(deck: Deck) {
        roomDatabase.deckDao().insertDeck(deck = deck.toRoomEntity())
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