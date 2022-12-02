package com.example.klaf.presentation.cardTransferring.common

sealed interface CardTransferringNavigationDestination {

    object CardMovingDialogDestination : CardTransferringNavigationDestination

    data class CardAddingFragmentDestination(
        val sourceDeckId: Int,
    ) : CardTransferringNavigationDestination

    data class CardDeletingDialogDestination(
        val cardQuantity: Int,
    ) : CardTransferringNavigationDestination

    object CardTransferringFragment : CardTransferringNavigationDestination

    data class CardEditingFragment(
        val cardId: Int,
        val deckId: Int,
    ) : CardTransferringNavigationDestination
}
