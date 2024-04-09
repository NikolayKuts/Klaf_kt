package com.kuts.klaf.presentation.deckRepetition

import com.kuts.klaf.presentation.deckRepetitionInfo.RepetitionInfoEvent

sealed class RepetitionScreenState {

    object StartState : RepetitionScreenState()

    object RepetitionState : RepetitionScreenState()

    data class FinishState(val repetitionInfoEvent: RepetitionInfoEvent) : RepetitionScreenState()
}
