package com.example.klaf.data.implementations

import android.content.Context
import com.example.klaf.data.dao.KlafRoomDatabase
import com.example.klaf.data.repositories.DeckListRepository
import com.example.klaf.domain.pojo.Deck
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeckListRepositoryRoomImp(context: Context) : DeckListRepository {
    private val database = KlafRoomDatabase.getInstance(context)

    override suspend fun getDataFormSours(): List<Deck> = withContext(Dispatchers.IO) {
        database.deckDao().getAllDecks()
    }

    override suspend fun insertDeck(deck: Deck) {
        withContext(Dispatchers.IO) {
            database.deckDao().insertDeck(deck)
        }
    }

    override suspend fun removeDeck(deckId: Int) {
        withContext(Dispatchers.IO) {
            database.deckDao().deleteDeck(deckId)
        }
    }
}