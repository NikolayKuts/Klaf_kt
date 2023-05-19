package com.kuts.klaf.presentation.deckList.common

sealed interface DeckListNavigationDestination {

    object DataSynchronizationDialog : DeckListNavigationDestination

    object Unspecified : DeckListNavigationDestination
}
