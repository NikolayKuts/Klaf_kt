package com.example.klaf.presentation.deckList.common

import com.example.domain.entities.Deck

sealed interface DeckListNavigationDestination {

    object DeckCreationDialog : DeckListNavigationDestination

    data class DeckRepetitionScreen(val deck: Deck) : DeckListNavigationDestination

    data class DeckNavigationDialog(val deck: Deck) : DeckListNavigationDestination

    object DataSynchronizationDialog : DeckListNavigationDestination

    data class CardTransferringScreen(val deckId: Int) : DeckListNavigationDestination

    object SigningTypeChoosingDialog : DeckListNavigationDestination
}
