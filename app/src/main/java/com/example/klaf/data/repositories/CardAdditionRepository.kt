package com.example.klaf.data.repositories

import androidx.lifecycle.LiveData
import com.example.klaf.domain.pojo.Card
import com.example.klaf.domain.pojo.Deck

interface CardAdditionRepository {

    suspend fun getCardQuantityByDeckId(deckId: Int): LiveData<Int>

    suspend fun insertCard(card: Card)

    fun getObservableDeckById(deckId: Int): LiveData<Deck?>
}