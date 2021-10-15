package com.example.klaf.data.repositories

import androidx.lifecycle.LiveData
import com.example.klaf.domain.pojo.Card
import com.example.klaf.domain.pojo.Deck

interface RepetitionRepository {

    fun getCardsByDeckId(deckId: Int): LiveData<List<Card>>

    suspend fun deleteCard(cardId: Int)

    suspend fun getDeckById(deckId: Int): Deck
}