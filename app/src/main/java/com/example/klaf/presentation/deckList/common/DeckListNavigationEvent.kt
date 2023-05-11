package com.example.klaf.presentation.deckList.common

import com.example.domain.entities.Deck

sealed interface DeckListNavigationEvent {

    object ToDeckCreationDialog : DeckListNavigationEvent

    data class ToDeckRepetitionScreen(val deck: Deck) : DeckListNavigationEvent

    data class ToDeckNavigationDialog(val deck: Deck) : DeckListNavigationEvent

    object ToDataSynchronizationDialog : DeckListNavigationEvent

    object ToSigningTypeChoosingDialog : DeckListNavigationEvent

    data class ToCardTransferringScreen(val deckId: Int) : DeckListNavigationEvent

    object ToPrevious : DeckListNavigationEvent
}
