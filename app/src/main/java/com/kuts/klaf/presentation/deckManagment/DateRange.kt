package com.kuts.klaf.presentation.deckManagment

enum class DateRange(val range: IntRange) {

    Year(range = 0..30),
    Month(range = 0..12),
    Week(range = 0..4),
    Day(range = 0..7),
    Hour(range = 0..24),
    Minute(range = 0..60);

    operator fun contains(value: Int): Boolean {
        return this.range.contains(value)
    }

    fun getValueWithinRange(value: Int): Int {
        return if (value < range.first) return range.first else range.last
    }

    fun getValueWithinRange(
        currentValue: Int,
        buttonAction: DraggableButtonAction
    ): Int = when (buttonAction) {
        DraggableButtonAction.Decrease -> {
            if (currentValue <= range.first) range.last else currentValue - 1
        }

        DraggableButtonAction.Increase -> {
            if (currentValue >= range.last) range.first else currentValue + 1
        }

        DraggableButtonAction.Reset -> {
            range.first
        }
    }
}