package com.example.klaf.presentation.interimDeck

sealed interface InterimDeckNavigationDestination {

    object CardMovingDialogDestination : InterimDeckNavigationDestination

    data class CardAddingFragmentDestination(val interimDeckId: Int) : InterimDeckNavigationDestination

    object CardDeletingDialogDestination : InterimDeckNavigationDestination
}
