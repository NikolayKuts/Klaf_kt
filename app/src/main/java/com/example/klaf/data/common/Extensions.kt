package com.example.klaf.data.common

import com.example.klaf.data.dataStore.DeckRepetitionInfo
import com.example.klaf.data.dataStore.DeckRepetitionSuccessMark
import com.example.klaf.data.dataStore.DeckRepetitionSuccessMark.FAILURE
import com.example.klaf.data.dataStore.DeckRepetitionSuccessMark.SUCCESS
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
