package com.example.klaf.presentation.cardViewing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.klaf.domain.entities.Card
import com.example.klaf.domain.useCases.FetchCardsUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class CardViewingViewModel @AssistedInject constructor(
    @Assisted deckId: Int,
    fetchCards: FetchCardsUseCase,
) : ViewModel() {

    val cards: StateFlow<List<Card>> = fetchCards(deckId = deckId).stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )
}