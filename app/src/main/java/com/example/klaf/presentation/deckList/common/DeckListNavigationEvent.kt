package com.example.klaf.presentation.deckList.common

import com.example.klaf.domain.entities.Deck

sealed class DeckListNavigationEvent {

    object ToDeckCreationDialog : DeckListNavigationEvent()

    data class ToFragment(val deck: Deck) : DeckListNavigationEvent()

    data class ToDeckNavigationDialog(val deck: Deck) : DeckListNavigationEvent()

    object ToDataSynchronizationDialog : DeckListNavigationEvent()
}
