package com.kuts.klaf.presentation.cardManagement.cardEditing

import androidx.lifecycle.ViewModel
import com.kuts.domain.common.LoadingState
import com.kuts.domain.entities.Card
import com.kuts.domain.entities.Deck
import com.kuts.domain.ipa.IpaHolder
import com.kuts.domain.ipa.LetterInfo
import com.kuts.klaf.data.networking.CardAudioPlayer
import com.kuts.klaf.presentation.cardManagement.cardAddition.AutocompleteState
import com.kuts.klaf.presentation.cardManagement.cardAddition.NativeWordSuggestionsState
import com.kuts.klaf.presentation.common.EventMessage
import com.kuts.klaf.presentation.common.EventMessageSource
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

abstract class BaseCardEditingViewModel : ViewModel(), EventMessageSource {

    abstract val deck: SharedFlow<Deck?>
    abstract val card: SharedFlow<Card?>
    abstract val audioPlayer: CardAudioPlayer
    abstract val cardEditingState: StateFlow<CardEditingState>
    abstract val autocompleteState: StateFlow<AutocompleteState>
    abstract val pronunciationLoadingState: StateFlow<LoadingState<Unit>>
    abstract val nativeWordSuggestionsState: StateFlow<NativeWordSuggestionsState>

    abstract fun updateCard(
        oldCard: Card,
        deckId: Int,
        nativeWord: String,
        foreignWord: String,
        letterInfos: List<LetterInfo>,
        ipaHolders: List<IpaHolder>,
    )

    abstract fun pronounce()

    abstract fun updateEditingState(word: String)

    abstract fun setSelectedAutocomplete(selectedWord: String)

    abstract fun closeAutocompleteMenu()

    abstract fun manageNativeWordSuggestionsState(state: NativeWordSuggestionsState)

    abstract fun sendEventMessage(message: EventMessage)
}