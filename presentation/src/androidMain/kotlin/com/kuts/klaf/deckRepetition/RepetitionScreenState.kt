package com.kuts.klaf.deckRepetition

import com.kuts.klaf.deckRepetitionInfo.RepetitionInfoEvent
import kotlinx.serialization.Serializable

@Serializable
sealed class RepetitionScreenState {
    @Serializable
    data object StartState : RepetitionScreenState()
    @Serializable
    data object RepetitionState : RepetitionScreenState()
    @Serializable
    data class FinishState(val repetitionInfoEvent: RepetitionInfoEvent) : RepetitionScreenState()
}
