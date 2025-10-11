package com.kuts.klaf.presentation.cardTransferring.common

sealed interface CardTransferringNavigationDestination {

    data object CardMovingDialog : CardTransferringNavigationDestination

    data object CardAddingScreen : CardTransferringNavigationDestination

    data object CardDeletionDialog : CardTransferringNavigationDestination

    data object CardTransferringScreen : CardTransferringNavigationDestination

    data class CardEditingScreen(
        val selectedCardIndexIndex: Int,
    ) : CardTransferringNavigationDestination
}
