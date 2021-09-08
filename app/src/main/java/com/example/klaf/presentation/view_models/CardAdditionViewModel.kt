package com.example.klaf.presentation.view_models

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.klaf.data.implementations.CardAdditionRepositoryRoomImp
import com.example.klaf.data.repositories.CardAdditionRepository
import com.example.klaf.domain.pojo.Card
import kotlinx.coroutines.launch

class CardAdditionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CardAdditionRepository = CardAdditionRepositoryRoomImp(application)

    fun addNewCard(card: Card) {
        viewModelScope.launch { repository.insertCard(card) }
    }
}