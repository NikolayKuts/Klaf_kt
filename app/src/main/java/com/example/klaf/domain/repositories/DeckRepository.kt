package com.example.klaf.domain.repositories

import com.example.klaf.domain.entities.Deck
import kotlinx.coroutines.flow.Flow

interface DeckRepository {

    fun fetchDeckSource(): Flow<List<Deck>>

    suspend fun fetchAllDecks(): List<Deck>

    fun fetchObservableDeckById(deckId: Int): Flow<Deck?>

    suspend fun insertDeck(deck: Deck)

    suspend fun removeDeck(deckId: Int)

    suspend fun getDeckById(deckId: Int): Deck?

    suspend fun getCardQuantityInDeck(deckId: Int): Int
}