package com.kuts.klaf.presentation.cardTransferring.common

sealed interface ICardTransferringNavigationDestination {

    data object CardMovingDialog : ICardTransferringNavigationDestination

    data object CardAddingScreen : ICardTransferringNavigationDestination

    data object CardDeletionDialog : ICardTransferringNavigationDestination

    data object CardTransferringScreen : ICardTransferringNavigationDestination

    data class CardEditingScreen(
        val selectedCardIndexIndex: Int,
    ) : ICardTransferringNavigationDestination
}
