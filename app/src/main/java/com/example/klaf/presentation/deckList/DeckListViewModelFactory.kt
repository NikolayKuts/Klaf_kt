package com.example.klaf.presentation.deckList

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.assisted.AssistedFactory

class DeckListViewModelFactory(
    private val assistedFactory: DeckListViewModelAssistedFactory,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return assistedFactory.create() as T
    }
}

@AssistedFactory
interface DeckListViewModelAssistedFactory {

    fun create(): DeckListViewModel
}