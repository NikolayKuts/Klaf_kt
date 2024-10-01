package com.kuts.klaf.presentation.cardTransferring.common

import com.kuts.domain.entities.Deck

sealed interface CardTransferringAction {

    data object ForeignWordVisibilityIconClick: CardTransferringAction

    data object NativeWordVisibilityIconClick: CardTransferringAction

    data class ChangeSelectionState(val position: Int): CardTransferringAction

    data object ChangeAllCardSelection: CardTransferringAction

    data class NavigateTo(val destination: CardTransferringNavigationDestination): CardTransferringAction

    data object DeleteCards: CardTransferringAction

    data class MoveCards(val targetDeck: Deck): CardTransferringAction

    data class PronounceWord(val wordIndex: Int): CardTransferringAction
}