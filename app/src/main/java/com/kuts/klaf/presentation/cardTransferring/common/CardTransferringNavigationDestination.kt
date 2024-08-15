package com.kuts.klaf.presentation.cardTransferring.common

sealed interface CardTransferringNavigationDestination {

    data object CardMovingDialog : CardTransferringNavigationDestination

    data object CardAddingFragment : CardTransferringNavigationDestination

    data object CardDeletionDialog : CardTransferringNavigationDestination

    data object CardTransferringScreen : CardTransferringNavigationDestination

    data class CardEditingFragment(
        val selectedCardIndexIndex: Int,
    ) : CardTransferringNavigationDestination
}
