package com.example.klaf.data.repositories

import androidx.lifecycle.LiveData
import com.example.klaf.domain.pojo.Card

interface CardViewerRepository {

    fun getCardsByDeckId(deckId: Int): LiveData<List<Card>>

    suspend fun insertCard(card: Card)

}