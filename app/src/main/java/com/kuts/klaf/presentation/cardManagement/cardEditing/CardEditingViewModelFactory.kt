package com.kuts.klaf.presentation.cardManagement.cardEditing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kuts.klaf.presentation.cardManagement.cardEditing.CardEditingViewModel.Companion.CARD_ARGUMENT_NAME
import com.kuts.klaf.presentation.cardManagement.cardEditing.CardEditingViewModel.Companion.DECK_ARGUMENT_NAME
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory

class CardEditingViewModelFactory(
    private val assistedFactory: CardEditingAssistedViewModelFactory,
    private val deckId: Int,
    private val cardId: Int,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return assistedFactory.crate(deckId = deckId, cardId = cardId) as T
    }
}

@AssistedFactory
interface CardEditingAssistedViewModelFactory {

    fun crate(
        @Assisted(DECK_ARGUMENT_NAME) deckId: Int,
        @Assisted(CARD_ARGUMENT_NAME) cardId: Int
    ): CardEditingViewModel
}