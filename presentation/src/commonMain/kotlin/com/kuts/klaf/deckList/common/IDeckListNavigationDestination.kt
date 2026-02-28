package com.kuts.klaf.deckList.common

sealed interface IDeckListNavigationDestination {

    data object DataSynchronizationDialog : IDeckListNavigationDestination

    data object Unspecified : IDeckListNavigationDestination
}
