package com.example.klaf.domain.repositories

import androidx.lifecycle.LiveData
import com.example.klaf.domain.entities.Card

interface CardRepository {

    suspend fun getCardQuantityByDeckId(deckId: Int): LiveData<Int>

    suspend fun insertCard(card: Card)

    suspend fun getCardById(cardId: Int): Card

    fun getCardsByDeckId(deckId: Int): LiveData<List<Card>>

    suspend fun deleteCard(cardId: Int)

    suspend fun removeCardsOfDeck(deckId: Int)
}