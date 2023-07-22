package com.kuts.domain.enums

enum class DayIncreaseFactor(val value: Float) {

    FIRST_DAY_INCREASE_FACTOR(0.4F),
    SECOND_DAY_INCREASE_FACTOR(0.6F),
    THIRD_DAY_INCREASE_FACTOR(0.8F),
    WHOLE_DAY_INCREASE_FACTOR(1.0F)
}