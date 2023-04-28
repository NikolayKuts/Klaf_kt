package com.example.klaf.presentation.cardManagement.cardAddition

import androidx.lifecycle.ViewModel
import com.example.domain.common.LoadingState
import com.example.domain.entities.Deck
import com.example.klaf.data.networking.CardAudioPlayer
import com.example.klaf.presentation.common.EventMessageSource
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

abstract class BaseCardAdditionViewModel : ViewModel(), EventMessageSource {

    abstract val deck: SharedFlow<Deck?>
    abstract val cardAdditionState: StateFlow<CardAdditionState>
    abstract val audioPlayer: CardAudioPlayer
    abstract val autocompleteState: StateFlow<AutocompleteState>
    abstract val pronunciationLoadingState: StateFlow<LoadingState<Unit>>

    abstract  fun sendEvent(event: CardAdditionEvent)
}