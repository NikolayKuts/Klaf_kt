package com.example.klaf.presentation.cardEdinting

import androidx.lifecycle.ViewModel
import com.example.klaf.domain.entities.Card
import com.example.klaf.domain.useCases.FetchDeckByIdUseCase

class CardEditingViewModel(
    deckId: Int,
    cardId: Int,
    private val fetchDeckById: FetchDeckByIdUseCase
) : ViewModel() {

//    private val repository: CardEditingRepository = CardEditingRepositoryRoomImp(context)

    val deck = fetchDeckById(deckId = deckId)

    fun onGetCardById(cardId: Int, onGetCard: (Card?) -> Unit) {
//        viewModelScope.launch { onGetCard(repository.getCardById(cardId)) }
    }

    fun insertChangedCard(card: Card) {
//        viewModelScope.launch { repository.insertCard(card) }
    }
}