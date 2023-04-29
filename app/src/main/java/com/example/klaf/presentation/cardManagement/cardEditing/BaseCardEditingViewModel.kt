package com.example.klaf.presentation.cardManagement.cardEditing

import androidx.lifecycle.ViewModel
import com.example.domain.common.LoadingState
import com.example.domain.entities.Card
import com.example.domain.entities.Deck
import com.example.domain.ipa.IpaHolder
import com.example.domain.ipa.LetterInfo
import com.example.klaf.data.networking.CardAudioPlayer
import com.example.klaf.presentation.cardManagement.cardAddition.AutocompleteState
import com.example.klaf.presentation.common.EventMessageSource
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

abstract class BaseCardEditingViewModel : ViewModel(), EventMessageSource {

    abstract val deck: SharedFlow<Deck?>
    abstract val card: SharedFlow<Card?>
    abstract val audioPlayer: CardAudioPlayer
    abstract val cardEditingState: StateFlow<CardEditingState>
    abstract val autocompleteState: StateFlow<AutocompleteState>
    abstract val pronunciationLoadingState: StateFlow<LoadingState<Unit>>

    abstract fun updateCard(
        oldCard: Card,
        deckId: Int,
        nativeWord: String,
        foreignWord: String,
        letterInfos: List<LetterInfo>,
        ipaHolders: List<IpaHolder>,
    )

    abstract fun pronounce()

    abstract fun preparePronunciation(word: String)

    abstract fun updateAutocompleteState(word: String)

    abstract fun setSelectedAutocomplete(selectedWord: String)

    abstract fun closeAutocompleteMenu()
}