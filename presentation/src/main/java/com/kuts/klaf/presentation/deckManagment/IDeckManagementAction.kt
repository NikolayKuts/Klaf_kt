package com.kuts.klaf.presentation.deckManagment

import com.kuts.domain.common.DateUnit

sealed interface IDeckManagementAction {

    data object ScheduledDateIntervalChangeRequested : IDeckManagementAction

    data object DismissScheduledDateIntervalDialog : IDeckManagementAction

    data object ScheduledDateIntervalChangeConfirmed : IDeckManagementAction

    data class ScheduledDateIntervalChanged(
        val dateUnit: DateUnit,
        val buttonAction: IDraggableButtonAction,
    ) : IDeckManagementAction

}
