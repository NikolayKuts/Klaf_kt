package com.kuts.klaf.presentation.deckRepetitionInfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.assisted.AssistedFactory

class DeckRepetitionInfoViewModelFactory(
    private val assistedFactory: DeckRepetitionInfoViewModelAssistedFactory,
    private val deckId: Int,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return assistedFactory.create(deckId = deckId) as T
    }
}

@AssistedFactory
interface DeckRepetitionInfoViewModelAssistedFactory {

    fun create(deckId: Int): DeckRepetitionInfoViewModel
}