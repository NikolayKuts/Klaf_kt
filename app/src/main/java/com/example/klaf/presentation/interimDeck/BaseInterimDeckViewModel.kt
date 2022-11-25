package com.example.klaf.presentation.interimDeck

import androidx.lifecycle.ViewModel
import com.example.klaf.domain.entities.Deck
import com.example.klaf.presentation.common.EventMessageSource
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

abstract class BaseInterimDeckViewModel : ViewModel(), EventMessageSource {

    abstract val interimDeck: SharedFlow<Deck?>
    abstract val cardHolders: StateFlow<List<SelectableCardHolder>>
    abstract val navigationDestination: SharedFlow<InterimDeckNavigationDestination>

    abstract fun changeSelectionState(position: Int)
    abstract fun selectAllCards()
    abstract fun navigate(event: InterimDeckNavigationEvent)
}