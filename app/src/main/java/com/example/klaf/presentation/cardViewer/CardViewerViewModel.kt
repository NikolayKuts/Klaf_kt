package com.example.klaf.presentation.cardViewer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.klaf.domain.entities.Card

class CardViewerViewModel(application: Application, private val deckId: Int) :
    AndroidViewModel(application) {

//    private val repository: CardViewerRepository = CardViewerRepositoryRoomImp(application)

//    val carsSours: LiveData<List<Card>> = repository.getCardsByDeckId(deckId)

    fun addNewCard(card: Card) {
//        viewModelScope.launch { repository.insertCard(card) }
    }

}