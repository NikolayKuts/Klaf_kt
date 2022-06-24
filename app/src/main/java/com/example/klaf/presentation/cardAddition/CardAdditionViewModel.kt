package com.example.klaf.presentation.cardAddition

import androidx.lifecycle.*
import com.example.klaf.domain.entities.Card
import com.example.klaf.domain.useCases.CreateCardUseCase
import com.example.klaf.domain.useCases.FetchDeckByIdUseCase
import kotlinx.coroutines.launch

class CardAdditionViewModel(
    deckId: Int,
    private val fetchDeckById: FetchDeckByIdUseCase,
    private val createCard: CreateCardUseCase
) : ViewModel() {

//    val deck: SharedFlow<Deck?> = fetchDeckByIdUseCase(deckId = deckId)

    fun addNewCard(card: Card) {
        viewModelScope.launch { createCard(card) }
    }
}