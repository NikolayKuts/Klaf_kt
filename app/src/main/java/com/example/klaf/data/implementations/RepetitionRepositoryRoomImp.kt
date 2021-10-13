package com.example.klaf.data.implementations

import android.content.Context
import androidx.lifecycle.LiveData
import com.example.klaf.data.dao.KlafRoomDatabase
import com.example.klaf.data.repositories.RepetitionRepository
import com.example.klaf.domain.pojo.Card
import com.example.klaf.domain.pojo.Deck
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RepetitionRepositoryRoomImp(context: Context) : RepetitionRepository {

    private val database = KlafRoomDatabase.getInstance(context)

    override suspend fun getCardByDeckId(deckId: Int): LiveData<List<Card>> {
        return withContext(Dispatchers.IO) {
            database.cardDao().getAllCardByDeckId(deckId = deckId)
        }
    }

    override suspend fun deleteCard(cardId: Int) {
        withContext(Dispatchers.IO) {

            val cardForDeleting = database.cardDao().getCardById(cardId = cardId)
            database.cardDao().deleteCard(cardId)

            val updatedDeck = database.deckDao()
                .getDeckById(cardForDeleting.deckId)
                .apply { cardQuantity -= 1 }
            database.deckDao().insertDeck(updatedDeck)
        }
    }

    override suspend fun getDeckById(deckId: Int): Deck = withContext(Dispatchers.IO) {
        database.deckDao().getDeckById(deckId)
    }
}