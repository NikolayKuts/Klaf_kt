package com.example.klaf.data.common

import com.example.domain.common.DeckRepetitionSuccessMark
import com.example.domain.common.DeckRepetitionSuccessMark.FAILURE
import com.example.domain.common.DeckRepetitionSuccessMark.SUCCESS
import com.example.domain.common.UNASSIGNED_LONG_VALUE
import com.example.domain.common.UNASSIGNED_STRING_VALUE
import com.example.domain.entities.Deck
import com.example.domain.entities.DeckRepetitionInfo
import com.example.klaf.R
import com.example.klaf.presentation.common.timeAsString

val Deck.lastIterationSuccessMark: DeckRepetitionSuccessMark
    get() = if (this.isLastIterationSucceeded) SUCCESS else FAILURE

val DeckRepetitionInfo.currentDurationAsTimeOrUnassigned: String
    get() {
        return if (this.isCurrentDurationUnassigned) {
            UNASSIGNED_STRING_VALUE
        } else {
            this.currentDuration.timeAsString
        }
    }

val DeckRepetitionInfo.isCurrentDurationUnassigned: Boolean
    get() = currentDuration == UNASSIGNED_LONG_VALUE

val DeckRepetitionSuccessMark.markResId: Int
    get() = when (this) {
        DeckRepetitionSuccessMark.UNASSIGNED -> R.string.unassigned_string_value
        SUCCESS -> R.string.deck_repetition_mark_successful
        FAILURE -> R.string.deck_repetition_mark_failed
    }
