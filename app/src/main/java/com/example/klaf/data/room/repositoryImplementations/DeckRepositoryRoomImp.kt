package com.example.klaf.data.room.repositoryImplementations

import com.example.klaf.data.room.databases.KlafRoomDatabase
import com.example.klaf.data.room.entities.RoomDeck
import com.example.klaf.data.room.mapToDeck
import com.example.klaf.data.room.mapToRoomEntity
import com.example.klaf.domain.common.simplifiedItemMap
import com.example.klaf.domain.entities.Deck
import com.example.klaf.domain.repositories.DeckRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeckRepositoryRoomImp @Inject constructor(
    private val roomDatabase: KlafRoomDatabase
) : DeckRepository {

    override fun fetchDeckSource(): Flow<List<Deck>> {
        return roomDatabase.deckDao()
            .getAllDecks()
            .simplifiedItemMap { roomDeck -> roomDeck.mapToDeck() }
    }

    override fun fetchObservableDeckById(deckId: Int): Flow<Deck?> {
        return roomDatabase.deckDao()
            .getObservableDeckById(deckId = deckId)
            .map { roomDeck: RoomDeck? -> roomDeck?.mapToDeck() }
    }

    override suspend fun insertDeck(deck: Deck) {
        withContext(Dispatchers.IO) {
            roomDatabase.deckDao().insertDeck(deck = deck.mapToRoomEntity())
        }
    }

    override suspend fun removeDeck(deckId: Int) {
        withContext(Dispatchers.IO) { roomDatabase.deckDao().deleteDeck(deckId) }
    }

    override suspend fun getDeckById(deckId: Int): Deck? = withContext(Dispatchers.IO) {
        roomDatabase.deckDao().getDeckById(deckId)?.mapToDeck()
    }

    override suspend fun getCardQuantityInDeck(deckId: Int): Int = withContext(Dispatchers.IO) {
        roomDatabase.cardDao().getCardQuantityInDeck(deckId)
    }
}