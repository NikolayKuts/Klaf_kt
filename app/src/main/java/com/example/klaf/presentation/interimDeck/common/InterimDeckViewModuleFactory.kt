package com.example.klaf.presentation.interimDeck.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.assisted.AssistedFactory

class InterimDeckViewModuleFactory(
    private val assistedFactory: InterimDeckViewModelAssistedFactory,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return assistedFactory.create() as T
    }
}

@AssistedFactory
interface InterimDeckViewModelAssistedFactory {

    fun create(): InterimDeckViewModel
}