package com.kuts.klaf.data.common

import com.kuts.domain.common.DeckReviewPassSuccessMark
import com.kuts.domain.common.DeckReviewPassSuccessMark.FAILURE
import com.kuts.domain.common.DeckReviewPassSuccessMark.SUCCESS
import com.kuts.domain.common.UNASSIGNED_LONG_VALUE
import com.kuts.domain.common.UNASSIGNED_STRING_VALUE
import com.kuts.domain.entities.Deck
import com.kuts.domain.entities.DeckRepetitionInfo
import com.kuts.klaf.R
import com.kuts.klaf.presentation.common.timeAsString

val Deck.lastReviewPassSuccessMark: DeckReviewPassSuccessMark
    get() = if (this.isLastPassSucceeded) SUCCESS else FAILURE

val DeckRepetitionInfo.currentDurationAsTimeOrUnassigned: String
    get() = if (this.isCurrentDurationUnassigned) {
        UNASSIGNED_STRING_VALUE
    } else {
        this.currentDuration.timeAsString
    }

val DeckRepetitionInfo.isCurrentDurationUnassigned: Boolean
    get() = currentDuration == UNASSIGNED_LONG_VALUE

val DeckReviewPassSuccessMark.markResId: Int
    get() = when (this) {
        DeckReviewPassSuccessMark.UNASSIGNED -> R.string.unassigned_string_value
        SUCCESS -> R.string.deck_repetition_mark_successful
        FAILURE -> R.string.deck_repetition_mark_failed
    }