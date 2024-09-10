package com.kuts.klaf.presentation.deckManagment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.assisted.AssistedFactory

class DeckManagementViewModelFactory(
    private val assistedFactory: DeckManagementAssistedViewModelFactory,
    private val deckId: Int,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return assistedFactory.create(deckId = deckId) as T
    }
}

@AssistedFactory
interface DeckManagementAssistedViewModelFactory {

    fun create(deckId: Int): DeckManagementViewModel
}