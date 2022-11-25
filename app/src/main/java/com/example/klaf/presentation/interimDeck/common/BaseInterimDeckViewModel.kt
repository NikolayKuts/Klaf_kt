package com.example.klaf.presentation.interimDeck.common

import androidx.lifecycle.ViewModel
import com.example.klaf.domain.entities.Deck
import com.example.klaf.presentation.common.EventMessageSource
import com.example.klaf.presentation.interimDeck.cardDeleting.CardDeletingState
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

abstract class BaseInterimDeckViewModel : ViewModel(), EventMessageSource {

    abstract val interimDeck: SharedFlow<Deck?>
    abstract val cardHolders: StateFlow<List<SelectableCardHolder>>
    abstract val navigationDestination: SharedFlow<InterimDeckNavigationDestination>
    abstract val cardDeletingState: StateFlow<CardDeletingState>

    abstract fun changeSelectionState(position: Int)
    abstract fun selectAllCards()
    abstract fun navigate(event: InterimDeckNavigationEvent)
    abstract fun deleteCards()
    abstract fun resetCardDeletingState()
}