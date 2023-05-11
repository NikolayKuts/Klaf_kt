package com.example.klaf.presentation.cardTransferring.common

sealed interface CardTransferringNavigationDestination {

    object CardMovingDialog : CardTransferringNavigationDestination

    object CardAddingFragment : CardTransferringNavigationDestination

    object CardDeletionDialog : CardTransferringNavigationDestination

    data class CardEditingFragment(
        val selectedCardIndexIndex: Int,
    ) : CardTransferringNavigationDestination
}
