package com.example.klaf.data.repositories

import androidx.lifecycle.LiveData
import com.example.klaf.domain.pojo.Card

interface CardViewerRepository {

    suspend fun getCardsByDeckId(deckId: Int): List<Card>

    suspend fun insertCard(card: Card)
}