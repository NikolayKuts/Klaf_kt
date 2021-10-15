package com.example.klaf.data.implementations

import android.content.Context
import androidx.lifecycle.LiveData
import com.example.klaf.data.dao.KlafRoomDatabase
import com.example.klaf.data.repositories.CardViewerRepository
import com.example.klaf.domain.pojo.Card
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CardViewerRepositoryRoomImp(context: Context) : CardViewerRepository {

    private val database = KlafRoomDatabase.getInstance(context)

    override fun getCardsByDeckId(deckId: Int): LiveData<List<Card>> {
        return database.cardDao().getAllCardByDeckId(deckId)
    }

    override suspend fun insertCard(card: Card) {
        withContext(Dispatchers.IO) { database.cardDao().insetCard(card) }
    }
}