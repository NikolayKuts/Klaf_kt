package com.example.klaf.presentation.view_models

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.klaf.data.implementations.RepetitionRepositoryRoomImp
import com.example.klaf.data.repositories.RepetitionRepository
import com.example.klaf.domain.pojo.Card
import kotlinx.coroutines.launch

class RepetitionViewModel(context: Context, deckId: Int) : ViewModel() {

    private val repository: RepetitionRepository = RepetitionRepositoryRoomImp(context)
    private val _cardSours = MutableLiveData<List<Card>>()
    val cardSours: LiveData<List<Card>> = _cardSours

    init {
        viewModelScope.launch { _cardSours.value =  repository.getCardByDeckId(deckId) }
    }
}