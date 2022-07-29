package com.example.klaf.domain.repositories

import androidx.lifecycle.LiveData
import com.example.klaf.domain.entities.Card
import kotlinx.coroutines.flow.Flow

interface CardRepository {

    suspend fun getObservableCardQuantityByDeckId(deckId: Int): LiveData<Int>

    suspend fun getCardQuantityByDeckId(deckId: Int): Int

    suspend fun insertCard(card: Card)

    fun getObservableCardById(cardId: Int): Flow<Card?>

    fun getCardsByDeckId(deckId: Int): Flow<List<Card>>

    suspend fun deleteCard(cardId: Int)

    suspend fun removeCardsOfDeck(deckId: Int)
}