package com.kuts.klaf.presentation.deckList.common

sealed interface DeckListNavigationDestination {

    data object DataSynchronizationDialog : DeckListNavigationDestination

    data object Unspecified : DeckListNavigationDestination
}
