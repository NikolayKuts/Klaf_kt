package com.example.klaf.data.repositories

import androidx.lifecycle.LiveData
import com.example.klaf.domain.pojo.Deck
import kotlinx.coroutines.flow.Flow

interface DeckListRepository {

    fun getDeckSource(): Flow<List<Deck>>

    suspend fun insertDeck(deck: Deck)

    suspend fun removeDeck(deckId: Int)

    suspend fun getDeckById(deckId: Int): Deck?

    suspend fun removeCardsOfDeck(deckId: Int)

    suspend fun getCardQuantityInDeck(deckId: Int): Int
}