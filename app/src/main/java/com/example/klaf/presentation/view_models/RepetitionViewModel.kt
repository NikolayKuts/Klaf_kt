package com.example.klaf.presentation.view_models

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.klaf.data.implementations.RepetitionRepositoryRoomImp
import com.example.klaf.data.repositories.RepetitionRepository
import com.example.klaf.domain.pojo.Card
import com.example.klaf.domain.pojo.Deck
import kotlinx.coroutines.launch

class RepetitionViewModel(context: Context, private val deckId: Int) : ViewModel() {

    private val repository: RepetitionRepository = RepetitionRepositoryRoomImp(context)

    fun onGetCardSource(callback: (LiveData<List<Card>>) -> Unit) {
        viewModelScope.launch { callback(repository.getCardByDeckId(deckId = deckId)) }
    }

    fun removeCard(cardId: Int) {
        viewModelScope.launch { repository.deleteCard(cardId = cardId) }
    }

    fun onGetDeck(deckId: Int, callback: (Deck) -> Unit) {
        viewModelScope.launch { callback(repository.getDeckById(deckId = deckId)) }
    }
}