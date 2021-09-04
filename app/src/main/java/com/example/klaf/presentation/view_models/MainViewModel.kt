package com.example.klaf.presentation.view_models

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.klaf.data.implementations.DeckListRepositoryRoomImp
import com.example.klaf.domain.pojo.Deck
import com.example.klaf.data.repositories.DeckListRepository
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DeckListRepository = DeckListRepositoryRoomImp(application)
    private var _deckSours = MutableLiveData<List<Deck>>()
    val deckSours: LiveData<List<Deck>> get() = _deckSours

    init { viewModelScope.launch { _deckSours.value = repository.getDataFormSours() } }

    fun addNewDeck(deck: Deck) {
        viewModelScope.launch { repository.insertDeck(deck) }
    }

    fun updateData() {
        viewModelScope.launch { _deckSours.value = repository.getDataFormSours() }
    }

}