package com.example.klaf.presentation.view_models

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.klaf.data.implementations.DeckListRepositoryRoomImp
import com.example.klaf.domain.pojo.Deck
import com.example.klaf.data.repositories.DeckListRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DeckListRepository = DeckListRepositoryRoomImp(application)
//    private var _deckSours: LiveData<List<Deck>> = MutableLiveData()
//    val deckSours: LiveData<List<Deck>> get() = _deckSours
//    private var deckSours: LiveData<List<Deck>> = MutableLiveData()

    fun onGetDeckSours(callback: (LiveData<List<Deck>>) -> Unit) {
        viewModelScope.launch { callback(repository.getDataFormSours()) }
    }

//    init {
//        viewModelScope.launch { deckSours = repository.getDataFormSours() }
//    }

    fun addNewDeck(deck: Deck) {
        viewModelScope.launch { repository.insertDeck(deck) }
    }

//    fun updateData() {
//        viewModelScope.launch { _deckSours.value = repository.getDataFormSours() }
//    }

    fun removeDeck(deckId: Int) {
        viewModelScope.launch { repository.removeDeck(deckId) }
    }

    fun onGetDeckById(deckId: Int, onDeckRetrieved: (Deck) -> Unit) {
        viewModelScope.launch {
            val deck = repository.getDeckById(deckId)
            onDeckRetrieved(deck)
        }
    }

}