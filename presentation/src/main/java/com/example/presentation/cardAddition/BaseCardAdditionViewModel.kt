package com.example.presentation.cardAddition

import androidx.lifecycle.ViewModel
import com.example.domain.entities.Deck
import com.example.presentation.common.EventMessageSource
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

abstract class BaseCardAdditionViewModel : ViewModel(), EventMessageSource {

    abstract val deck: SharedFlow<Deck?>
    abstract val cardAdditionState: StateFlow<CardAdditionState>
    abstract val autocompleteState: StateFlow<AutocompleteState>

    abstract  fun sendEvent(event: CardAdditionEvent)
}