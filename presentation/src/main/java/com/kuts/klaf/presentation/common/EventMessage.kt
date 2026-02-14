package com.kuts.klaf.presentation.common

import androidx.annotation.StringRes

class EventMessage(
    @StringRes val resId: Int,
    vararg val args: Any = emptyArray(),
    val type: Type = Type.Neutral,
    val duration: Duration = Duration.Medium,
) {

    enum class Type { Negative, Waring, Neutral, Positive }

    enum class Duration {

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
