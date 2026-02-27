package com.kuts.klaf.deckList.common

import com.kuts.domain.entities.Deck
import com.kuts.klaf.common.EventMessage
import com.kuts.klaf.common.NavigationDestination
import com.kuts.klaf.deckList.drawer.DrawerAction

sealed interface IDeckListNavigationEvent {

    data object ToDeckCreationDialog : IDeckListNavigationEvent

    data class ToDeckRepetitionScreen(val deck: Deck) : IDeckListNavigationEvent

    data class ToDeckNavigationDialog(val deck: Deck) : IDeckListNavigationEvent

    data object ToDataSynchronizationDialog : IDeckListNavigationEvent

    data class ToSigningTypeChoosingDialog(
        val fromSourceDestination: NavigationDestination,
    ) : IDeckListNavigationEvent

    data class ToCardTransferringScreen(val deckId: Int) : IDeckListNavigationEvent

    data object ToPrevious : IDeckListNavigationEvent

    data class ToDrawerActionDialog(val action: DrawerAction) : IDeckListNavigationEvent

    data class ToChatGptWithDeckContentPrompt(
        val foreignWords: String,
        val event: EventMessage
    ) : IDeckListNavigationEvent
}
