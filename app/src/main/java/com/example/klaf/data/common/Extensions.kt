package com.example.klaf.data.common

import com.example.klaf.domain.entities.DeckRepetitionInfo
import com.example.klaf.domain.entities.DeckRepetitionSuccessMark
import com.example.klaf.domain.entities.DeckRepetitionSuccessMark.FAILURE
import com.example.klaf.domain.entities.DeckRepetitionSuccessMark.SUCCESS
import com.example.klaf.domain.common.UNASSIGNED_LONG_VALUE
import com.example.klaf.domain.common.UNASSIGNED_STRING_VALUE
import com.example.klaf.domain.entities.Deck
import com.example.klaf.presentation.common.timeAsString

val Deck.lastIterationSuccessMark: DeckRepetitionSuccessMark
    get() {
        return if (this.isLastIterationSucceeded) SUCCESS else FAILURE
    }

val DeckRepetitionInfo.currentDurationAsTimeOrUnassigned: String
    get() {
        return if (this.isCurrentDurationUnassigned) {
            UNASSIGNED_STRING_VALUE
        } else {
            this.currentDuration.timeAsString
        }
    }

val DeckRepetitionInfo.isCurrentDurationUnassigned: Boolean
    get() {
        return this.currentDuration == UNASSIGNED_LONG_VALUE
    }
