package com.example.klaf.presentation.interimDeck.common

sealed interface InterimDeckNavigationDestination {

    object CardMovingDialogDestination : InterimDeckNavigationDestination

    data class CardAddingFragmentDestination(
        val interimDeckId: Int,
    ) : InterimDeckNavigationDestination

    data class CardDeletingDialogDestination(
        val cardQuantity: Int,
    ) : InterimDeckNavigationDestination
}
