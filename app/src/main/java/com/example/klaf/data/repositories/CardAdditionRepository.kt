package com.example.klaf.data.repositories

import androidx.lifecycle.LiveData
import com.example.klaf.domain.pojo.Card

interface CardAdditionRepository {

    suspend fun getCardQuantityByDeckId(deckId: Int): LiveData<Int>

    suspend fun onInsertCard(card: Card)
}