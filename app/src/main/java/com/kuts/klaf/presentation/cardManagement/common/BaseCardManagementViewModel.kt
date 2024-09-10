package com.kuts.klaf.presentation.cardManagement.common

import androidx.lifecycle.ViewModel
import com.kuts.domain.common.LoadingState
import com.kuts.domain.entities.Deck
import com.kuts.klaf.data.networking.CardAudioPlayer
import com.kuts.klaf.presentation.cardManagement.cardAddition.AutocompleteState
import com.kuts.klaf.presentation.cardManagement.cardAddition.NativeWordSuggestionsState
import com.kuts.klaf.presentation.common.EventMessageSource
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

abstract class BaseCardManagementViewModel(
    val audioPlayer: CardAudioPlayer,
) : ViewModel(), EventMessageSource {

    abstract val deck: SharedFlow<Deck?>
    abstract val autocompleteState: StateFlow<AutocompleteState>
    abstract val pronunciationLoadingState: StateFlow<LoadingState<Unit>>
    abstract val nativeWordSuggestionsState: StateFlow<NativeWordSuggestionsState>
    abstract val transcriptionState: StateFlow<String>
    abstract val cardManagementState: StateFlow<CardManagementState>

    abstract fun sendEvent(event: CardManagementEvent)
}