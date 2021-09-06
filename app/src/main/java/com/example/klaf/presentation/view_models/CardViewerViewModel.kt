package com.example.klaf.presentation.view_models

import android.app.Application
import android.content.Context
import androidx.lifecycle.*
import com.example.klaf.data.implementations.CardViewerRepositoryRoomImp
import com.example.klaf.data.repositories.CardViewerRepository
import com.example.klaf.domain.pojo.Card
import kotlinx.coroutines.launch

class CardViewerViewModel(application: Application, private val deckId: Int) :
    AndroidViewModel(application) {

    private val repository: CardViewerRepository = CardViewerRepositoryRoomImp(application)
    private var _cardSours = MutableLiveData<List<Card>>()
    val carsSours: LiveData<List<Card>> = _cardSours

    init { viewModelScope.launch { _cardSours.value = repository.getCardsByDeckId(deckId) } }

    fun addNewCard(card: Card) {
        viewModelScope.launch { repository.insertCard(card) }
    }

}