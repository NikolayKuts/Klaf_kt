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

    override fun getCardsByDeckId(deckId: Int): LiveData<List<Card>> {
        return database.cardDao().getAllCardByDeckId(deckId = deckId)
    }

    override suspend fun deleteCard(cardId: Int) {
        withContext(Dispatchers.IO) {

            val cardForDeleting = database.cardDao().getCardById(cardId = cardId)
            database.cardDao().deleteCard(cardId)

            val updatedDeck = database.deckDao()
                .getDeckById(cardForDeleting.deckId)
                ?.run {
                    copy(cardQuantity = this.cardQuantity - 1)
                } ?: throw RuntimeException("No found a deck for creating a new one")

            database.deckDao().insertDeck(updatedDeck)
        }
    }

    override suspend fun getDeckById(deckId: Int): Deck? = withContext(Dispatchers.IO) {
        database.deckDao().getDeckById(deckId)
    }
}