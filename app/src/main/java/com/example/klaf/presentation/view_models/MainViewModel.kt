package com.example.klaf.presentation.view_models

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.klaf.data.implementations.DeckListRepositoryRoomImp
import com.example.klaf.domain.pojo.Deck
import com.example.klaf.data.repositories.DeckListRepository
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DeckListRepository = DeckListRepositoryRoomImp(application)

    val deckSource: LiveData<List<Deck>> = repository.getDeckSource()

    fun addNewDeck(deck: Deck) {
        viewModelScope.launch { repository.insertDeck(deck = deck) }
    }

    fun onRemoveDeck(deckId: Int) {
        viewModelScope.launch {
            repository.removeDeck(deckId = deckId)
            repository.removeCardsOfDeck(deckId = deckId)
        }
    }

    fun onGetDeckById(deckId: Int, onDeckRetrieved: (Deck?) -> Unit) {
        viewModelScope.launch {
            val deck = repository.getDeckById(deckId)
            onDeckRetrieved(deck)
        }
    }

    fun isDeckNotEmpty(deckId: Int, onResponse: (Boolean) -> Unit) {
        viewModelScope.launch {
            val cardQuantity = repository.getCardQuantityInDeck(deckId)
            onResponse(cardQuantity > 0)
        }
    }

}