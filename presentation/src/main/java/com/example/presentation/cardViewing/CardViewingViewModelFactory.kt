package com.example.presentation.cardViewing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.assisted.AssistedFactory

class CardViewingViewModelFactory(
    private val assistedFactory: CardViewingViewModelAssistedFactory,
    private val deckId: Int,
) :
    ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return assistedFactory.create(deckId = deckId) as T
    }


    @AssistedFactory
    interface CardViewingViewModelAssistedFactory {

        fun create(deckId: Int): CardViewingViewModel
    }
}