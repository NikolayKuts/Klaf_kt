package com.example.klaf.data.repositories

import androidx.lifecycle.LiveData
import com.example.klaf.domain.pojo.Card
import com.example.klaf.domain.pojo.Deck

interface CardEditingRepository {

    suspend fun insertCard(card: Card)

    suspend fun getCardById(cardId: Int): Card

    fun getObservableDeckById(deckId: Int): LiveData<Deck?>
}