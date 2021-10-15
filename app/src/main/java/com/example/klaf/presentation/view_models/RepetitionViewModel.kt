package com.example.klaf.presentation.view_models

import android.content.Context
import androidx.lifecycle.*
import com.example.klaf.data.implementations.RepetitionRepositoryRoomImp
import com.example.klaf.data.repositories.RepetitionRepository
import com.example.klaf.domain.pojo.Card
import com.example.klaf.domain.pojo.Deck
import kotlinx.coroutines.launch

class RepetitionViewModel(context: Context, private val deckId: Int) : ViewModel() {

    private val repository: RepetitionRepository = RepetitionRepositoryRoomImp(context)

    val cardSource: LiveData<List<Card>> = repository.getCardsByDeckId(deckId)

    fun removeCard(cardId: Int) {
        viewModelScope.launch { repository.deleteCard(cardId = cardId) }
    }

    fun onGetDeck(deckId: Int, onDeckReceived: (Deck?) -> Unit) {
        viewModelScope.launch { onDeckReceived(repository.getDeckById(deckId = deckId)) }
    }
}