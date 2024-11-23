package com.kuts.klaf.presentation.deckManagment

sealed interface DeckManagementEvent {

    data object None : DeckManagementEvent

    data class ShowScheduledDateIntervalChangeDialog(val dateData: DateData) : DeckManagementEvent
}