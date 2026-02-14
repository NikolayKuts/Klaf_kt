package com.kuts.klaf.presentation.deckList.common

sealed interface IDeckListNavigationDestination {

    data object DataSynchronizationDialog : IDeckListNavigationDestination

    data object Unspecified : IDeckListNavigationDestination
}
