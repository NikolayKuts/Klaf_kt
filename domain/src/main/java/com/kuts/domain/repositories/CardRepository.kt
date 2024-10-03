package com.kuts.domain.repositories

import com.kuts.domain.entities.Card
import com.kuts.domain.entities.Deck
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

    suspend fun checkIfCardExists(foreignWord: String): List<Deck>
}