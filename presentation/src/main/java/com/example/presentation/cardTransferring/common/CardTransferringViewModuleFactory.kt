package com.example.presentation.cardTransferring.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.assisted.AssistedFactory

class CardTransferringViewModuleFactory(
    private val sourceDeckId: Int,
    private val assistedFactory: CardTransferringViewModelAssistedFactory,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return assistedFactory.create(sourceDeckId = sourceDeckId) as T
    }
}

@AssistedFactory
interface CardTransferringViewModelAssistedFactory {

    fun create(sourceDeckId: Int): CardTransferringViewModel
}