package com.example.klaf.presentation.cardEditing

import androidx.lifecycle.ViewModel
import com.example.klaf.domain.entities.Card
import com.example.klaf.domain.entities.Deck
import com.example.klaf.domain.ipa.LetterInfo
import com.example.klaf.presentation.cardAddition.AutocompleteState
import com.example.klaf.presentation.common.EventMessageSource
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

abstract class BaseCardEditingViewModel : ViewModel(), EventMessageSource {

    abstract val deck: SharedFlow<Deck?>
    abstract val card: SharedFlow<Card?>
    abstract val cardEditingState: StateFlow<CardEditingState>
    abstract val autocompleteState: StateFlow<AutocompleteState>

    abstract fun updateCard(
        oldCard: Card,
        deckId: Int,
        nativeWord: String,
        foreignWord: String,
        letterInfos: List<LetterInfo>,
        ipaTemplate: String,
    )

    abstract fun pronounce()

    abstract fun preparePronunciation(word: String)

    abstract fun updateAutocompleteState(word: String)

    abstract fun setSelectedAutocomplete(selectedWord: String)

    abstract fun closeAutocompleteMenu()
}