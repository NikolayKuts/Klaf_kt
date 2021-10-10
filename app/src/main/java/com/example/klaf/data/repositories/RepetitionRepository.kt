package com.example.klaf.data.repositories

import androidx.lifecycle.LiveData
import com.example.klaf.domain.pojo.Card

interface RepetitionRepository {

    suspend fun getCardByDeckId(deckId: Int): LiveData<List<Card>>

    suspend fun deleteCard(cardId: Int)
}