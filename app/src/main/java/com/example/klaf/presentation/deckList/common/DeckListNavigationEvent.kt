package com.example.klaf.presentation.deckList.common

import com.example.domain.entities.Deck

sealed interface DeckListNavigationEvent {

    object ToDeckCreationDialog : DeckListNavigationEvent

    data class ToFragment(val deck: Deck) : DeckListNavigationEvent

    data class ToDeckNavigationDialog(val deck: Deck) : DeckListNavigationEvent

    object ToDataSynchronizationDialog : DeckListNavigationEvent
}
