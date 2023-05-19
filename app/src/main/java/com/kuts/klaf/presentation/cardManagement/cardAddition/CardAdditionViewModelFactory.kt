package com.kuts.klaf.presentation.cardManagement.cardAddition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.assisted.AssistedFactory

class CardAdditionViewModelFactory(
    private val assistedFactory: CardAdditionViewModelAssistedFactory,
    private val deckId: Int,
    private val smartSelectedWord: String?,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return assistedFactory.create(
            deckId = deckId,
            smartSelectedWord = smartSelectedWord,
        ) as T
    }
}

@AssistedFactory
interface CardAdditionViewModelAssistedFactory {

    fun create(deckId: Int, smartSelectedWord: String?): CardAdditionViewModel
}