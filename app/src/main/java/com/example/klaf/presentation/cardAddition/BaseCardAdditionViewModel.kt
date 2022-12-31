package com.example.klaf.presentation.cardAddition

import androidx.lifecycle.ViewModel
import com.example.klaf.data.networking.CardAudioPlayer
import com.example.klaf.domain.entities.Deck
import com.example.klaf.presentation.common.EventMessageSource
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

abstract class BaseCardAdditionViewModel : ViewModel(), EventMessageSource {

    abstract val deck: SharedFlow<Deck?>
    abstract val cardAdditionState: StateFlow<CardAdditionState>
    abstract val audioPlayer: CardAudioPlayer
    abstract val autocompleteState: StateFlow<AutocompleteState>

    abstract  fun sendEvent(event: CardAdditionEvent)
}