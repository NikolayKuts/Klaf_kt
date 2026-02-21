package com.kuts.klaf.deckManagment

import com.kuts.domain.common.DateData

sealed interface IDeckManagementEvent {

    data object None : IDeckManagementEvent

    data class ShowScheduledDateIntervalChangeDialog(val dateData: DateData) : IDeckManagementEvent
}
