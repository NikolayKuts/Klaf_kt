package com.kuts.klaf.presentation.deckRepetition

import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.assisted.AssistedFactory

class RepetitionViewModelFactory(
    private val assistedFactory: IRepetitionViewModelAssistedFactory,
    private val deckId: Int,
) : AbstractSavedStateViewModelFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle
    ): T {
        return assistedFactory.create(deckId = deckId, handle = handle) as T
    }
}

@AssistedFactory
interface IRepetitionViewModelAssistedFactory {

    fun create(deckId: Int, handle: SavedStateHandle): DeckReviewViewModel
}