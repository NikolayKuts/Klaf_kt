package com.kuts.klaf.presentation.deckList.common

import com.kuts.domain.entities.Deck
import com.kuts.klaf.presentation.common.NavigationDestination

sealed interface DeckListNavigationEvent {

    object ToDeckCreationDialog : DeckListNavigationEvent

    data class ToDeckRepetitionScreen(val deck: Deck) : DeckListNavigationEvent

    data class ToDeckNavigationDialog(val deck: Deck) : DeckListNavigationEvent

    object ToDataSynchronizationDialog : DeckListNavigationEvent

    data class ToSigningTypeChoosingDialog(
        val fromSourceDestination: NavigationDestination,
    ) : DeckListNavigationEvent

    data class ToCardTransferringScreen(val deckId: Int) : DeckListNavigationEvent

    object ToPrevious : DeckListNavigationEvent
}
