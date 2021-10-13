package com.example.klaf.data.implementations

import android.content.Context
import androidx.lifecycle.LiveData
import com.example.klaf.data.dao.KlafRoomDatabase
import com.example.klaf.data.repositories.CardAdditionRepository
import com.example.klaf.domain.pojo.Card
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CardAdditionRepositoryRoomImp(context: Context) : CardAdditionRepository {

    private val database = KlafRoomDatabase.getInstance(context)

    override suspend fun getCardQuantityByDeckId(deckId: Int): LiveData<Int> {
        return withContext(Dispatchers.IO) { database.cardDao().getCardQuantityByDeckId(deckId) }
    }

    override suspend fun onInsertCard(card: Card) {
        withContext(Dispatchers.IO) {
            database.cardDao().insetCard(card)
            val cardQuantity = database.cardDao().getCardQuantityAsInt(card.deckId)
            val updatedDeck = database.deckDao().getDeckById(card.deckId)
                .apply {
                    this.cardQuantity = cardQuantity
                }
            database.deckDao().insertDeck(updatedDeck)
        }
    }
}