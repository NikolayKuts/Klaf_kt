package com.example.klaf.presentation.view_models

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.klaf.data.implementations.CardEditingRepositoryRoomImp
import com.example.klaf.data.repositories.CardEditingRepository
import com.example.klaf.domain.pojo.Card
import com.example.klaf.domain.pojo.Deck
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CardEditingViewModel(context: Context, deckId: Int, cardId: Int) : ViewModel() {

    private val repository: CardEditingRepository = CardEditingRepositoryRoomImp(context)

    val deck: LiveData<Deck?> = repository.getObservableDeckById(deckId = deckId)

    fun onGetCardById(cardId: Int, onGetCard: (Card?) -> Unit) {
        viewModelScope.launch { onGetCard(repository.getCardById(cardId)) }
    }

    fun insertChangedCard(card: Card) {
        viewModelScope.launch { repository.insertCard(card) }
    }
}