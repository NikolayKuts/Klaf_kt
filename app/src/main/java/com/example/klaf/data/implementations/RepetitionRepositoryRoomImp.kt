package com.example.klaf.data.implementations

import android.content.Context
import com.example.klaf.data.dao.KlafRoomDatabase
import com.example.klaf.data.repositories.RepetitionRepository
import com.example.klaf.domain.pojo.Card
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RepetitionRepositoryRoomImp(context: Context) : RepetitionRepository {

    private val database = KlafRoomDatabase.getInstance(context)

    override suspend fun getCardByDeckId(deckId: Int): List<Card> {
        return withContext(Dispatchers.IO) { database.cardDao().getAllCardByDeckId(deckId) }
    }
}