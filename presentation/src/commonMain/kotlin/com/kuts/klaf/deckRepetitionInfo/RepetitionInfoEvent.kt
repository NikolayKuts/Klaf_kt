package com.kuts.klaf.deckRepetitionInfo

import kotlinx.serialization.Serializable

@Serializable
enum class RepetitionInfoEvent {

    Non, ScheduledSuccessfully, SchedulingFailed, OneRepetitionToFinish
}
