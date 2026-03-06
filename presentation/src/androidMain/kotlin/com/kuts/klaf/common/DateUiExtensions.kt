package com.kuts.klaf.common

import androidx.compose.runtime.Composable
import com.kuts.domain.common.DateData
import com.kuts.domain.common.DateUnit
import com.kuts.domain.common.DeckReviewPassSuccessMark
import com.kuts.domain.common.DeckReviewPassSuccessMark.FAILURE
import com.kuts.domain.common.DeckReviewPassSuccessMark.SUCCESS
import com.kuts.domain.common.ScheduledDateState
import com.kuts.domain.common.UNASSIGNED_LONG_VALUE
import com.kuts.domain.common.UNASSIGNED_STRING_VALUE
import com.kuts.domain.entities.Deck
import com.kuts.domain.entities.DeckRepetitionInfo
import com.kuts.klaf.presentation.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import java.util.concurrent.TimeUnit

private const val UNASSIGNED_DATE_SYMBOL = "---"
private const val MINUS_SYMBOL = "-"

private val yearInMillis = TimeUnit.DAYS.toMillis(365)
private val monthInMillis = TimeUnit.DAYS.toMillis(31)
private val weekInMillis = TimeUnit.DAYS.toMillis(7)
private val dayInMillis = TimeUnit.DAYS.toMillis(1)
private val hourImMillis = TimeUnit.HOURS.toMillis(1)
private val minuteInMillis = TimeUnit.MINUTES.toMillis(1)

fun DateUnit.toLabelRes(): StringResource = when (this) {
    is DateUnit.Year -> Res.string.year
    is DateUnit.Month -> Res.string.month
    is DateUnit.Week -> Res.string.week
    is DateUnit.Day -> Res.string.day
    is DateUnit.Hour -> Res.string.hour
    is DateUnit.Minute -> Res.string.minute
}

@Composable
fun DateData.asString(): String {
    val years = getYearsOrEmpty(year.value)
    val months = getMonthsOrEmpty(month.value)
    val week = getWeeksOrEmpty(week.value)
    val days = getDaysOrEmpty(day.value)
    val hours = getHoursOrEmpty(hour.value)
    val minutes = getMinutesOrEmpty(minute.value)

    return buildString {
        setOf(years, months, week, days, hours, minutes).forEach {
            if (it.isNotEmpty()) append("$it ")
        }
    }
}

@Composable
fun Deck.calculateDetailedScheduledRange(): String {
    return scheduledDate.calculateDetailedScheduledRange()
}

@Composable
fun DeckRepetitionInfo.calculateDetailedScheduledRange(): String {
    return scheduledDate.calculateDetailedScheduledRange()
}

@Composable
fun DeckRepetitionInfo.calculateDetailedPreviousScheduledRange(): String {
    return previousScheduledDate.calculateDetailedScheduledRange()
}

@Composable
fun Long?.calculateDetailedScheduledRange(): String {
    if (this == null || this <= 0) return UNASSIGNED_DATE_SYMBOL
    val currentTime = System.currentTimeMillis()
    val range = this - currentTime

    val years = range.calculateYearQuantity().toInt()
    val months = range.calculateMonthQuantity().toInt()
    val weeks = range.calculateWeekQuantity().toInt()
    val days = range.calculateDayQuantity().toInt()
    val hours = range.calculateHoursQuantity().toInt()
    val minutes = range.calculateMinuteQuantity().toInt()

    val yearsMonthsWeeks =
        getDetailedYearsMonthsWeeks(years = years, months = months, weeks = weeks)
    val daysHoursMinutes =
        getDetailedDaysHoursMinutes(days = days, hours = hours, minutes = minutes)

    return yearsMonthsWeeks.ifEmpty {
        daysHoursMinutes.ifEmpty { stringResource(resource = Res.string.time_pointer_now) }
    }
}

@Composable
fun Deck.getScheduledDateStateByByCalculatedRange(): ScheduledDateState {
    val range = calculateDetailedScheduledRange()

    return ScheduledDateState(
        range = range,
        isOverdue = range.firstOrNull()?.toString() == MINUS_SYMBOL
    )
}

val DeckRepetitionInfo.currentDurationAsTimeOrUnassigned: String
    get() = if (currentDuration == UNASSIGNED_LONG_VALUE) {
        UNASSIGNED_STRING_VALUE
    } else {
        currentDuration.timeAsString
    }

val DeckReviewPassSuccessMark.markResId: StringResource
    get() = when (this) {
        DeckReviewPassSuccessMark.UNASSIGNED -> Res.string.unassigned_string_value
        SUCCESS -> Res.string.deck_repetition_mark_successful
        FAILURE -> Res.string.deck_repetition_mark_failed
    }

private fun Long.calculateYearQuantity(): Long = this / yearInMillis

private fun Long.calculateMonthQuantity(): Long = this % yearInMillis / monthInMillis

private fun Long.calculateWeekQuantity(): Long = this % yearInMillis % monthInMillis / weekInMillis

private fun Long.calculateDayQuantity(): Long {
    return this % yearInMillis % monthInMillis % weekInMillis / dayInMillis
}

private fun Long.calculateHoursQuantity(): Long {
    return this % yearInMillis % monthInMillis % weekInMillis % dayInMillis / hourImMillis
}

private fun Long.calculateMinuteQuantity(): Long {
    return this % yearInMillis %
        monthInMillis % weekInMillis % dayInMillis % hourImMillis / minuteInMillis
}

@Composable
private fun getDetailedYearsMonthsWeeks(years: Int, months: Int, weeks: Int): String {
    val range =
        "${getYearsOrEmpty(years)} ${getMonthsOrEmpty(months)} ${getWeeksOrEmpty(weeks)}".trim()

    if (range.isEmpty()) return ""
    return range.getFormatted()
}

@Composable
private fun getDetailedDaysHoursMinutes(days: Int, hours: Int, minutes: Int): String {
    val range = "${getDaysOrEmpty(days)} ${getHoursOrEmpty(hours)} ${getMinutesOrEmpty(minutes)}"
        .trim()

    if (range.isEmpty()) return ""
    return range.getFormatted()
}

private fun String.getFormatted(): String {
    return this.substring(range = 0..0) + this.substring(startIndex = 1, endIndex = this.length)
        .replace(oldValue = MINUS_SYMBOL, newValue = "")
}

@Composable
private fun getYearsOrEmpty(years: Int): String {
    return if (years == 0) "" else stringResource(resource = Res.string.year_pointer, years)
}

@Composable
private fun getMonthsOrEmpty(months: Int): String {
    return if (months == 0) "" else stringResource(resource = Res.string.month_pointer, months)
}

@Composable
private fun getWeeksOrEmpty(weeks: Int): String {
    return if (weeks == 0) "" else stringResource(resource = Res.string.week_pointer, weeks)
}

@Composable
private fun getDaysOrEmpty(days: Int): String {
    return if (days == 0) "" else stringResource(resource = Res.string.day_pointer, days)
}

@Composable
private fun getHoursOrEmpty(hours: Int): String {
    return if (hours == 0) "" else stringResource(resource = Res.string.hour_pointer, hours)
}

@Composable
private fun getMinutesOrEmpty(minutes: Int): String {
    return if (minutes == 0) "" else stringResource(resource = Res.string.minute_pointer, minutes)
}
