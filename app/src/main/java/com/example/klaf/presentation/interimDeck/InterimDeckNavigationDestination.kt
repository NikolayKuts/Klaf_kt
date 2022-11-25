package com.example.klaf.presentation.interimDeck

sealed interface InterimDeckNavigationDestination {

    object CardMovingDialogDestination : InterimDeckNavigationDestination

    object CardAddingFragmentDestination : InterimDeckNavigationDestination

    object CardDeletingDialogDestination : InterimDeckNavigationDestination
}
