package com.example.klaf.presentation.cardTransferring.common

import androidx.lifecycle.ViewModel
import com.example.klaf.domain.entities.Deck
import com.example.klaf.presentation.common.EventMessageSource
import com.example.klaf.presentation.cardTransferring.cardDeleting.CardDeletingState
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

abstract class BaseCardTransferringViewModel : ViewModel(), EventMessageSource {

    abstract val sourceDeck: SharedFlow<Deck?>
    abstract val cardHolders: StateFlow<List<SelectableCardHolder>>
    abstract val navigationDestination: SharedFlow<CardTransferringNavigationDestination>
    abstract val cardDeletingState: StateFlow<CardDeletingState>
    abstract val decks: StateFlow<List<Deck>>

    abstract fun changeSelectionState(position: Int)
    abstract fun selectAllCards()
    abstract fun navigate(event: CardTransferringNavigationEvent)
    abstract fun deleteCards()
    abstract fun moveCards(targetDeck: Deck)
    abstract fun resetCardDeletingState()
}