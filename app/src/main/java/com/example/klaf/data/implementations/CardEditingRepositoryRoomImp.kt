package com.example.klaf.data.implementations

import android.content.Context
import androidx.lifecycle.LiveData
import com.example.klaf.data.dao.KlafRoomDatabase
import com.example.klaf.data.repositories.CardEditingRepository
import com.example.klaf.domain.pojo.Card
import com.example.klaf.domain.pojo.Deck
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CardEditingRepositoryRoomImp(context: Context) : CardEditingRepository {

    private val database = KlafRoomDatabase.getInstance(context)

    override suspend fun insertCard(card: Card) {
        withContext(Dispatchers.IO) { database.cardDao().insetCard(card) }
    }

    override suspend fun getCardById(cardId: Int): Card = withContext(Dispatchers.IO) {
        database.cardDao().getCardById(cardId)
    }

    override fun getObservableDeckById(deckId: Int): LiveData<Deck?> {
        return database.deckDao().getObservableDeckById(deckId = deckId)
    }
}