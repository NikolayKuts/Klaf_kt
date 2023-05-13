package com.example.klaf.presentation.common

import androidx.annotation.StringRes

class EventMessage(
    @StringRes val resId: Int,
    val type: Type = Type.Neutral,
    val duration: EvenMessageDuration = EvenMessageDuration.Medium,
) {

    enum class Type { Negative, Waring, Neutral, Positive }

    enum class EvenMessageDuration {

        Short,
        Medium,
        Long;

        val value: kotlin.Long
            get() = when (this) {
                Short -> 2000L
                Medium -> 2600L
                Long -> 3200L
            }
    }
}
