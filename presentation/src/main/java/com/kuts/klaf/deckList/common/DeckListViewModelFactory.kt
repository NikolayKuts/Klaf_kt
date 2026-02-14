package com.kuts.klaf.deckList.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.assisted.AssistedFactory

class DeckListViewModelFactory(
    private val assistedFactory: IDeckListViewModelAssistedFactory,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return assistedFactory.create() as T
    }
}

@AssistedFactory
interface IDeckListViewModelAssistedFactory {

    fun create(): DeckListViewModel
}