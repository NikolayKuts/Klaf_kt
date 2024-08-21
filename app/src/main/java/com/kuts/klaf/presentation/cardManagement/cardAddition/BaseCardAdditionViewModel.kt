package com.kuts.klaf.presentation.cardManagement.cardAddition

import androidx.lifecycle.ViewModel
import com.kuts.domain.common.LoadingState
import com.kuts.domain.entities.Deck
import com.kuts.klaf.data.networking.CardAudioPlayer
import com.kuts.klaf.presentation.common.EventMessageSource
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

abstract class BaseCardAdditionViewModel : ViewModel(), EventMessageSource {

    abstract val deck: SharedFlow<Deck?>
    abstract val cardAdditionState: StateFlow<CardAdditionState>
    abstract val audioPlayer: CardAudioPlayer
    abstract val autocompleteState: StateFlow<AutocompleteState>
    abstract val pronunciationLoadingState: StateFlow<LoadingState<Unit>>
    abstract val nativeWordSuggestionsState: StateFlow<NativeWordSuggestionsState>

    abstract  fun sendEvent(event: CardAdditionEvent)
}