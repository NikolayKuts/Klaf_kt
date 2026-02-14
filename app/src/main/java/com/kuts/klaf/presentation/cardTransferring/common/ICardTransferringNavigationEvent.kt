package com.kuts.klaf.presentation.cardTransferring.common

sealed interface ICardTransferringNavigationEvent {

    object ToPrevious : ICardTransferringNavigationEvent

    object ToCardMovingDialog : ICardTransferringNavigationEvent

    data class ToCardAddingScreen(
        val sourceDeckId: Int,
    ) : ICardTransferringNavigationEvent

    data class ToCardEditingScreen(
        val cardId: Int,
        val deckId: Int,
    ) : ICardTransferringNavigationEvent

    data class ToCardDeletingDialog(
        val cardQuantity: Int,
    ) : ICardTransferringNavigationEvent
}