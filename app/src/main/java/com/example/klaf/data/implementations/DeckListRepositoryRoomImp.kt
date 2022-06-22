package com.example.klaf.data.implementations

import android.content.Context
import com.example.klaf.data.dao.KlafRoomDatabase
import com.example.klaf.data.repositories.DeckListRepository
import com.example.klaf.domain.pojo.Deck
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class DeckListRepositoryRoomImp(context: Context) : DeckListRepository {
    private val database = KlafRoomDatabase.getInstance(context)

    override fun getDeckSource(): Flow<List<Deck>> = database.deckDao().getAllDecks()

    override suspend fun insertDeck(deck: Deck) {
        withContext(Dispatchers.IO) { database.deckDao().insertDeck(deck) }
    }

    override suspend fun removeDeck(deckId: Int) {
        withContext(Dispatchers.IO) { database.deckDao().deleteDeck(deckId) }
    }

    override suspend fun getDeckById(deckId: Int): Deck? = withContext(Dispatchers.IO) {
        database.deckDao().getDeckById(deckId)
    }

    override suspend fun removeCardsOfDeck(deckId: Int) {
        withContext(Dispatchers.IO) { database.cardDao().deleteCardsByDeckId(deckId = deckId) }
    }

    override suspend fun getCardQuantityInDeck(deckId: Int): Int = withContext(Dispatchers.IO) {
        database.cardDao().getCardQuantityInDeck(deckId)
    }

}