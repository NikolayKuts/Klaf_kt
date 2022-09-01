package com.example.klaf.domain.repositories

import androidx.lifecycle.LiveData
import com.example.klaf.domain.entities.Card
import kotlinx.coroutines.flow.Flow

interface CardRepository {

    suspend fun fetchObservableCardQuantityByDeckId(deckId: Int): LiveData<Int>

    suspend fun fetchCardQuantityByDeckId(deckId: Int): Int

    suspend fun insertCard(card: Card)

    fun fetchObservableCardById(cardId: Int): Flow<Card?>

    fun fetchObservableCardsByDeckId(deckId: Int): Flow<List<Card>>

    fun fetchCardsByDeckId(deckId: Int): List<Card>

    suspend fun deleteCard(cardId: Int)

    suspend fun removeCardsOfDeck(deckId: Int)
}