package com.kuts.klaf.presentation.cardManagement.common

import androidx.lifecycle.ViewModel
import com.cambridge.dictionary.client.CambridgeClient
import com.cambridge.dictionary.core.Word
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
    protected val cambridgeClient: CambridgeClient,
) : ViewModel(), EventMessageSource {

    abstract val deck: SharedFlow<Deck?>
    abstract val autocompleteState: StateFlow<AutocompleteState>
    abstract val pronunciationLoadingState: StateFlow<LoadingState<Unit>>
    abstract val nativeWordSuggestionsState: StateFlow<NativeWordSuggestionsState>
    abstract val transcriptionState: StateFlow<String>
    abstract val cardManagementState: StateFlow<CardManagementState>

    abstract val cambridgeDataState: StateFlow<CambridgeDataState>

    abstract fun sendEvent(event: CardManagementEvent)
}

sealed interface CambridgeDataState {

    data class Fetched(val word: Word): CambridgeDataState

    data object Empty : CambridgeDataState
}