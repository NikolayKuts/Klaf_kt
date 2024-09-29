package com.kuts.klaf.presentation.deckManagment

sealed interface DeckManagementAction {

//    data class ScheduledDateIntervalChangeRequested(val dateData: DateData)
    data object ScheduledDateIntervalChangeRequested : DeckManagementAction

    data object DismissScheduledDateIntervalDialog : DeckManagementAction

    data object ScheduledDateIntervalChangeConfirmed : DeckManagementAction

    data class ScheduledDateIntervalChanged(
        val dateUnit: DateUnit,
        val value: String
    ) : DeckManagementAction

}