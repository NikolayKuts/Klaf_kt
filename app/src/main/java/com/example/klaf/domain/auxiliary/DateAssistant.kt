package com.example.klaf.domain.auxiliary

import com.example.klaf.domain.entities.Deck
import com.example.klaf.domain.enums.DayFactor.*
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

private const val DERATION_FACTOR = 0.07
private const val DECREASE_FACTOR = 0.1

private const val DATE_FORMAT_PATTERN = "dd-MM-yy|HH:mm"

object DateAssistant {

    fun getFormattedCurrentTime(): String {
        val date = Calendar.getInstance().time
        val dateFormat: DateFormat = SimpleDateFormat(DATE_FORMAT_PATTERN, Locale.getDefault())
        return dateFormat.format(date)
    }

    fun getFormattedDate(date: Long): String? {
        val dateFormat: DateFormat = SimpleDateFormat(DATE_FORMAT_PATTERN, Locale.getDefault())
        return dateFormat.format(date)
    }

    fun getCurrentDateAsLong(): Long {
        return Calendar.getInstance().time.time
    }

    fun getNextScheduledRepeatDate(deck: Deck, currentRepeatDuration: Long): Long {
        val lastRepeatInterval = getLastRepeatInterval(deck)

        return if (deck.repeatQuantity >= 5) {

            if (deck.repeatQuantity % 2 != 0) {
                val newInterval = getNewInterval(deck, currentRepeatDuration, lastRepeatInterval)
                getCurrentDateAsLong() + newInterval
            } else {
                deck.scheduledDate
            }
        } else {
            getCurrentDateAsLong()
        }
    }

    private fun getLastRepeatInterval(deck: Deck): Long {
        return if (deck.repeatQuantity == 5) {
            TimeUnit.MINUTES.toMillis(15)
        } else {
            deck.scheduledDate - deck.lastRepeatDate
        }
    }

    private fun getNewInterval(
        deck: Deck,
        currentRepeatDuration: Long,
        lastRepeatInterval: Long,
    ): Long {
        val lastRepeatDuration = deck.lastRepeatDuration

        return if (
            currentRepeatDuration <= lastRepeatDuration + lastRepeatDuration * DERATION_FACTOR
        ) {
            lastRepeatInterval + (lastRepeatInterval * getDayFactorByNumberDay(deck.repeatDay)).toLong()
        } else {

            if (deck.isLastRepetitionSucceeded) {
                lastRepeatInterval
            } else {
                lastRepeatInterval - lastRepeatInterval * DECREASE_FACTOR.toLong()
            }
        }
    }

    fun isRepetitionSucceeded(desk: Deck, currentRepetitionDuration: Long): Boolean {
        val lastRepetitionDuration: Long = desk.lastRepeatDuration
        return currentRepetitionDuration <= lastRepetitionDuration + lastRepetitionDuration * DERATION_FACTOR
    }

    fun getUpdatedRepeatDay(desk: Deck): Int {
        var date: Int = desk.repeatDay
        val difference: Long = getCurrentDateAsLong() - desk.creationDate
        val differenceInDays = difference / TimeUnit.DAYS.toMillis(1)
//        val differenceInDays = difference / 86400000
        return if (date < differenceInDays) ++date else date
    }

    private fun getDayFactorByNumberDay(numberDay: Int): Double {
        return when (numberDay) {
            1 -> FIRST_DAY_FACTOR.factor
            2 -> SECOND_DAY_FACTOR.factor
            3 -> THIRD_DAY_FACTOR.factor
            else -> WHOLE_DAY_FACTOR.factor
        }
    }
}





