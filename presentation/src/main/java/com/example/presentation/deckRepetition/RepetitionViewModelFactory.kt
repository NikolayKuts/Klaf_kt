package com.example.presentation.deckRepetition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.assisted.AssistedFactory

class RepetitionViewModelFactory(
    private val assistedFactory: RepetitionViewModelAssistedFactory,
    private val deckId: Int,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return assistedFactory.create(deckId = deckId) as T
    }
}

@AssistedFactory
interface RepetitionViewModelAssistedFactory {

    fun create(deckId: Int): DeckRepetitionViewModel
}