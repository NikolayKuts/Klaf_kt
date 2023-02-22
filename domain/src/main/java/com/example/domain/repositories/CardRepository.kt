package com.example.domain.repositories

import com.example.domain.entities.Card
import kotlinx.coroutines.flow.Flow

interface CardRepository {

    suspend fun fetchCardQuantityByDeckId(deckId: Int): Int

    suspend fun fetchAllCards(): List<Card>

    suspend fun insertCard(card: Card)

    fun fetchObservableCardById(cardId: Int): Flow<Card?>

    fun fetchObservableCardsByDeckId(deckId: Int): Flow<List<Card>>

    fun fetchCardsByDeckId(deckId: Int): List<Card>

    suspend fun deleteCard(cardId: Int)

    suspend fun removeCardsOfDeck(deckId: Int)
}