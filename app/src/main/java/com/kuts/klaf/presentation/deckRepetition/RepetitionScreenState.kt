package com.kuts.klaf.presentation.deckRepetition

import android.os.Parcelable
import com.kuts.klaf.presentation.deckRepetitionInfo.RepetitionInfoEvent
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class RepetitionScreenState : Parcelable {
    @Parcelize
    data object StartState : RepetitionScreenState()
    @Parcelize
    data object RepetitionState : RepetitionScreenState()
    @Parcelize
    data class FinishState(val repetitionInfoEvent: RepetitionInfoEvent) : RepetitionScreenState()
}
