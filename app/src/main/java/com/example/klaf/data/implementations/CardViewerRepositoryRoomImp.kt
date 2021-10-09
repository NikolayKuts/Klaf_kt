package com.example.klaf.data.implementations

import android.content.Context
import com.example.klaf.data.dao.KlafRoomDatabase
import com.example.klaf.data.repositories.CardViewerRepository
import com.example.klaf.domain.pojo.Card
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CardViewerRepositoryRoomImp(context: Context) : CardViewerRepository {

    private val database = KlafRoomDatabase.getInstance(context)

    override suspend fun getCardsByDeckId(deckId: Int): List<Card> {
        return withContext(Dispatchers.IO) { database.cardDao().getCardListByDeckId(deckId) }
    }

    override suspend fun insertCard(card: Card) {
        withContext(Dispatchers.IO) { database.cardDao().insetCard(card) }
    }
}