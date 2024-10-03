package com.kuts.klaf.presentation.deckManagment

data class DeckManagementState(
    val name: StatePair<String> = StatePair(pointer = "name", value = ""),
    val creationDate: StatePair<String> = StatePair(pointer = "creationDate", value = ""),
    val repetitionIterationDates: List<String> = emptyList(),
    val scheduledIterationDates: List<String> = emptyList(),
    val scheduledDateInterval: StatePair<DateData> = StatePair(
        pointer = "scheduledDateInterval",
        value = DateData()
    ),
    val repetitionQuantity: StatePair<String> = StatePair(
        pointer = "repetitionQuantity",
        value = ""
    ),
    val cardQuantity: StatePair<String> = StatePair(pointer = "cardQuantity", value = ""),
    val lastFirstRepetitionDuration: StatePair<String> = StatePair(
        pointer = "lastFirstRepetitionDuration",
        value = ""
    ),
    val lastSecondRepetitionDuration: StatePair<String> = StatePair(
        pointer = "lastSecondRepetitionDuration",
        value = ""
    ),
    val lastRepetitionIterationDuration: StatePair<String> = StatePair(
        pointer = "lastRepetitionIterationDuration",
        value = ""
    ),
    val isLastIterationSucceeded: StatePair<String> = StatePair(
        pointer = "isLastIterationSucceeded",
        value = ""
    ),
    val id: StatePair<String> = StatePair(pointer = "id", value = ""),
    val scheduledDateIntervalChangeState: ScheduledDataIntervalChangeState = ScheduledDataIntervalChangeState.NotRequired,
)

data class StatePair<T>(
    val pointer: String,
    val value: T,
)

sealed interface ScheduledDataIntervalChangeState {

    data object NotRequired : ScheduledDataIntervalChangeState

    data class Required(val dateData: DateData) : ScheduledDataIntervalChangeState
}

class DateDataValidator {

    fun validateForSaving(dateData: DateData): DateData {
        val getUpdatedDataData: (DateUnit) -> DateData = { dateUnit ->
            val updatedValue = dateUnit.getValueByRange()
            dateData.copyByUnit(unit = dateUnit, value = updatedValue)
        }

        return when {
            dateData.year.value !in DateRange.Year -> getUpdatedDataData(dateData.year)
            dateData.month.value !in DateRange.Month -> getUpdatedDataData(dateData.month)
            dateData.week.value !in DateRange.Week -> getUpdatedDataData(dateData.week)
            dateData.day.value !in DateRange.Day -> getUpdatedDataData(dateData.day)
            dateData.hour.value !in DateRange.Hour -> getUpdatedDataData(dateData.hour)
            dateData.minute.value !in DateRange.Minute -> getUpdatedDataData(dateData.minute)
            else -> dateData
        }
    }
}

fun DateUnit.getValueByRange(): Int {
    return when (this) {
        is DateUnit.Day -> DateRange.Day.getValueWithinRange(value = value)
        is DateUnit.Hour -> DateRange.Hour.getValueWithinRange(value = value)
        is DateUnit.Minute -> DateRange.Minute.getValueWithinRange(value = value)
        is DateUnit.Month -> DateRange.Month.getValueWithinRange(value = value)
        is DateUnit.Week -> DateRange.Week.getValueWithinRange(value = value)
        is DateUnit.Year -> DateRange.Year.getValueWithinRange(value = value)
    }
}

class ButtonActionHandler {

    fun handle(
        dateData: DateData,
        dataUnit: DateUnit,
        buttonAction: DraggableButtonAction,
    ): DateData {
        val updatedValue = dataUnit.getValueByRange(buttonAction = buttonAction)
        return dateData.copyByUnit(unit = dataUnit, value = updatedValue)
    }

    private fun DateUnit.getValueByRange(buttonAction: DraggableButtonAction): Int {
        return when (this) {
            is DateUnit.Year -> DateRange.Year.getValueWithinRange(
                currentValue = value,
                buttonAction = buttonAction
            )

            is DateUnit.Month -> DateRange.Month.getValueWithinRange(
                currentValue = value,
                buttonAction = buttonAction
            )

            is DateUnit.Week -> DateRange.Week.getValueWithinRange(
                currentValue = value,
                buttonAction = buttonAction
            )

            is DateUnit.Day -> DateRange.Day.getValueWithinRange(
                currentValue = value,
                buttonAction = buttonAction
            )

            is DateUnit.Hour -> DateRange.Hour.getValueWithinRange(
                currentValue = value,
                buttonAction = buttonAction
            )

            is DateUnit.Minute -> DateRange.Minute.getValueWithinRange(
                currentValue = value,
                buttonAction = buttonAction
            )
        }
    }
}