package com.example.klaf.data.repositories

import androidx.lifecycle.LiveData
import com.example.klaf.domain.pojo.Deck

interface DeckListRepository {

    suspend fun getDataFormSours(): LiveData<List<Deck>>

    suspend fun insertDeck(deck: Deck)

    suspend fun removeDeck(deckId: Int)

    suspend fun getDeckById(deckId: Int): Deck
}