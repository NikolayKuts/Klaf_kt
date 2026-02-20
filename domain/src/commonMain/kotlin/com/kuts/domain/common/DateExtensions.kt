package com.kuts.domain.common

import com.kuts.domain.common.DeckReviewPassSuccessMark.FAILURE
import com.kuts.domain.common.DeckReviewPassSuccessMark.SUCCESS
import com.kuts.domain.entities.Deck
import com.kuts.domain.entities.Deck.Companion.MIN_SCHEDULED_REPETITION_INTERVAL_MINUTES
import com.kuts.domain.enums.DayIncreaseFactor.FIRST_DAY_INCREASE_FACTOR
import com.kuts.domain.enums.DayIncreaseFactor.SECOND_DAY_INCREASE_FACTOR
import com.kuts.domain.enums.DayIncreaseFactor.THIRD_DAY_INCREASE_FACTOR
import com.kuts.domain.enums.DayIncreaseFactor.WHOLE_DAY_INCREASE_FACTOR

private const val ADDITIONAL_ALLOWABLE_DURATION_FACTOR = 0.068F
private const val DECREASE_FACTOR = 0.2F

private const val MILLIS_IN_MINUTE = 60_000L
private const val MILLIS_IN_HOUR = 60L * MILLIS_IN_MINUTE
private const val MILLIS_IN_DAY = 24L * MILLIS_IN_HOUR
private const val MILLIS_IN_WEEK = 7L * MILLIS_IN_DAY
private const val MILLIS_IN_MONTH = 31L * MILLIS_IN_DAY
private const val MILLIS_IN_YEAR = 365L * MILLIS_IN_DAY

private val yearInMillis = MILLIS_IN_YEAR
private val monthInMillis = MILLIS_IN_MONTH
private val weekInMillis = MILLIS_IN_WEEK
private val dayInMillis = MILLIS_IN_DAY
private val hourImMillis = MILLIS_IN_HOUR
private val minuteInMillis = MILLIS_IN_MINUTE

fun Deck.calculateNextScheduledRepeatDate(currentRepetitionIterationDuration: Long): Long {
    return if (reviewCount >= 5) {
        getCurrentDateAsLong() + getNewInterval(currentRepetitionIterationDuration)
    } else {
        getCurrentDateAsLong()
    }
}

fun Deck.getNewInterval(currentIterationDuration: Long): Long {
    val minScheduledRepetitionInterval = MIN_SCHEDULED_REPETITION_INTERVAL_MINUTES.toMillis()

    if (reviewCount < 5) return 0
    if (scheduledDateInterval <= 0L) return minScheduledRepetitionInterval

    val shouldIntervalBeIncreased = isRepetitionIterationSucceeded(currentIterationDuration)

    return if (shouldIntervalBeIncreased) {
        calculateIncreasedInterval(currentDuration = currentIterationDuration)
    } else {
        calculateDecreasedInterval(currentIterationDuration = currentIterationDuration)
    }
}

private fun Deck.calculateIncreasedInterval(currentDuration: Long): Long {
    val baseFactor = getDayIncreaseFactorByDayQuantity(quantity = existenceDayQuantity)
    val dynamicFactor = calculateDynamicIncreaseFactor(
        lastDuration = lastReviewPassDuration,
        currentDuration = currentDuration,
    )
    val finalFactor = baseFactor * dynamicFactor
    val increaseInterval = (scheduledDateInterval * finalFactor).toLong()

    return scheduledDateInterval + increaseInterval
}

private fun calculateDynamicIncreaseFactor(
    lastDuration: Long,
    currentDuration: Long,
): Float {
    if (currentDuration <= lastDuration) return 1.0f

    val maxAllowedDuration = lastDuration + (lastDuration * ADDITIONAL_ALLOWABLE_DURATION_FACTOR)
    if (currentDuration >= maxAllowedDuration) return 0f

    val overTimeRatio =
        (currentDuration - lastDuration).toFloat() / (maxAllowedDuration - lastDuration)
    return 1.0f - overTimeRatio
}

private fun Deck.calculateDecreasedInterval(currentIterationDuration: Long): Long {
    val minScheduledRepetitionInterval = MIN_SCHEDULED_REPETITION_INTERVAL_MINUTES.toMillis()
    val multiplicationFactor = (currentIterationDuration.toFloat() / lastReviewPassDuration)
    val actualDecreaseFactor = DECREASE_FACTOR * multiplicationFactor
    val decreaseInterval = (scheduledDateInterval * actualDecreaseFactor).toLong()
    val decreasedInterval = scheduledDateInterval - decreaseInterval

    return if (
        decreaseInterval >= scheduledDateInterval
        || decreasedInterval <= minScheduledRepetitionInterval
    ) {
        minScheduledRepetitionInterval
    } else {
        decreasedInterval
    }
}

private fun Long.toMillis(): Long = this * MILLIS_IN_MINUTE

fun Deck.isRepetitionIterationSucceeded(currentRepetitionDuration: Long): Boolean {
    val maxRepetitionIterationDuration =
        lastReviewPassDuration + lastReviewPassDuration * ADDITIONAL_ALLOWABLE_DURATION_FACTOR
    return currentRepetitionDuration <= maxRepetitionIterationDuration
}

fun Deck.getMaxTime(): Long {
    val maxRepetitionIterationDuration =
        lastReviewPassDuration + lastReviewPassDuration * ADDITIONAL_ALLOWABLE_DURATION_FACTOR
    return maxRepetitionIterationDuration.toLong()
}

val Deck.lastReviewPassSuccessMark: DeckReviewPassSuccessMark
    get() = if (isLastPassSucceeded) SUCCESS else FAILURE

private fun getDayIncreaseFactorByDayQuantity(quantity: Long): Float {
    return when (quantity) {
        1L -> FIRST_DAY_INCREASE_FACTOR.value
        2L -> SECOND_DAY_INCREASE_FACTOR.value
        3L -> THIRD_DAY_INCREASE_FACTOR.value
        else -> WHOLE_DAY_INCREASE_FACTOR.value
    }
}

fun Long.calculateDetailedScheduledInterval(): DateData {
    return DateData(
        DateUnit.Year(value = calculateYearQuantity().toInt()),
        DateUnit.Month(value = calculateMonthQuantity().toInt()),
        DateUnit.Week(value = calculateWeekQuantity().toInt()),
        DateUnit.Day(value = calculateDayQuantity().toInt()),
        DateUnit.Hour(value = calculateHoursQuantity().toInt()),
        DateUnit.Minute(value = calculateMinuteQuantity().toInt()),
    )
}

fun DateData.calculateDetailedScheduledIntervalAsLong(): Long {
    val millisInMinute = 60 * 1000L
    val millisInHour = 60 * millisInMinute
    val millisInDay = 24 * millisInHour
    val millisInWeek = 7 * millisInDay
    val millisInMonth = 30 * millisInDay
    val millisInYear = 365 * millisInDay

    val yearMillis = year.value * millisInYear
    val monthMillis = month.value * millisInMonth
    val weekMillis = week.value * millisInWeek
    val dayMillis = day.value * millisInDay
    val hourMillis = hour.value * millisInHour
    val minuteMillis = minute.value * millisInMinute

    return yearMillis + monthMillis + weekMillis + dayMillis + hourMillis + minuteMillis
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
