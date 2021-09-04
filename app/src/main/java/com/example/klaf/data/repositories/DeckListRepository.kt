package com.example.klaf.data.repositories

import com.example.klaf.domain.pojo.Deck

interface DeckListRepository {
    suspend fun getDataFormSours(): List<Deck>
    suspend fun insertDeck(deck: Deck)
}