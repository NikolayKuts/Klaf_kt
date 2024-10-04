package com.kuts.klaf.presentation.deckRepetition

import com.kuts.klaf.presentation.deckRepetitionInfo.RepetitionInfoEvent

sealed class RepetitionScreenState {

    data object StartState : RepetitionScreenState()

    data object RepetitionState : RepetitionScreenState()

    data class FinishState(val repetitionInfoEvent: RepetitionInfoEvent) : RepetitionScreenState()
}
