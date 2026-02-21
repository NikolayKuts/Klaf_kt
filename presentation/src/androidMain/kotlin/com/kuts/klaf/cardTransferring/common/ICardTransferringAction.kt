package com.kuts.klaf.cardTransferring.common

import com.kuts.domain.entities.Deck

sealed interface ICardTransferringAction {

    data object ForeignWordVisibilityIconClick: ICardTransferringAction

    data object NativeWordVisibilityIconClick: ICardTransferringAction

    data class ChangeSelectionState(val position: Int): ICardTransferringAction

    data object ChangeAllCardSelection: ICardTransferringAction

    data class NavigateTo(val destination: ICardTransferringNavigationDestination): ICardTransferringAction

    data object DeleteCards: ICardTransferringAction

    data class MoveCards(val targetDeck: Deck): ICardTransferringAction

    data class PronounceWord(val wordIndex: Int): ICardTransferringAction
}