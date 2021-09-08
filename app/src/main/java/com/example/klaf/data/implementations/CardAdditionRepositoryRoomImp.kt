package com.example.klaf.data.implementations

import android.content.Context
import com.example.klaf.data.dao.KlafRoomDatabase
import com.example.klaf.data.repositories.CardAdditionRepository
import com.example.klaf.domain.pojo.Card
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CardAdditionRepositoryRoomImp(context: Context) : CardAdditionRepository {

    private val database = KlafRoomDatabase.getInstance(context)

    override suspend fun insertCard(card: Card) {
        withContext(Dispatchers.IO) { database.cardDao().insetCard(card) }
    }
}