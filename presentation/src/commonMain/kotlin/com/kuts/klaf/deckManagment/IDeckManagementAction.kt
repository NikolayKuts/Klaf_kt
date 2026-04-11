package com.kuts.klaf.deckManagment

import com.kuts.domain.common.DateUnit

sealed interface IDeckManagementAction {

    data object ScheduledDateIntervalChangeRequested : IDeckManagementAction

    data object DismissScheduledDateIntervalDialog : IDeckManagementAction

    data object ScheduledDateIntervalChangeConfirmed : IDeckManagementAction

    data class ScheduledDateIntervalChanged(
        val dateUnit: DateUnit,
        val buttonAction: IDraggableButtonAction,
    ) : IDeckManagementAction

    data object ScheduledReviewChangeRequested : IDeckManagementAction

    data object DismissScheduledReviewDialog : IDeckManagementAction

    data object ScheduledReviewChangeConfirmed : IDeckManagementAction

    data class ScheduledReviewChanged(
        val dateUnit: DateUnit,
        val buttonAction: IDraggableButtonAction,
    ) : IDeckManagementAction

}
