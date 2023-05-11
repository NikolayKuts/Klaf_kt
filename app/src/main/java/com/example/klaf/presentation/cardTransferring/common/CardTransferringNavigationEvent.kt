package com.example.klaf.presentation.cardTransferring.common

sealed interface CardTransferringNavigationEvent {

    object ToCardTransferringScreen : CardTransferringNavigationEvent

    object ToCardMovingDialog : CardTransferringNavigationEvent

    data class ToCardAddingScreen(
        val sourceDeckId: Int,
    ) : CardTransferringNavigationEvent

    data class ToCardEditingScreen(
        val cardId: Int,
        val deckId: Int,
    ) : CardTransferringNavigationEvent

    data class ToCardDeletingDialog(
        val cardQuantity: Int,
    ) : CardTransferringNavigationEvent
}