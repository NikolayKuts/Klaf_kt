package com.example.klaf.presentation.card_viewer

import android.app.Application
import androidx.lifecycle.*
import com.example.klaf.data.implementations.CardViewerRepositoryRoomImp
import com.example.klaf.data.repositories.CardViewerRepository
import com.example.klaf.domain.pojo.Card
import kotlinx.coroutines.launch

class CardViewerViewModel(application: Application, private val deckId: Int) :
    AndroidViewModel(application) {

    private val repository: CardViewerRepository = CardViewerRepositoryRoomImp(application)

    val carsSours: LiveData<List<Card>> = repository.getCardsByDeckId(deckId)

    fun addNewCard(card: Card) {
        viewModelScope.launch { repository.insertCard(card) }
    }

}