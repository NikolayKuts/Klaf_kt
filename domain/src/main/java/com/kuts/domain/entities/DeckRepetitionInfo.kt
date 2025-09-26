package com.kuts.domain.entities

import com.kuts.domain.common.DeckReviewPassSuccessMark
import kotlinx.serialization.Serializable

@Serializable
data class DeckRepetitionInfo(
    val deckId: Int,
    val currentDuration: Long,
    val previousDuration: Long,
    val scheduledDate: Long,
    val previousScheduledDate: Long,
    val lastIterationDate: Long?,
    val repetitionQuantity: Int,
    val currentIterationSuccessMark: DeckReviewPassSuccessMark,
    val previousIterationSuccessMark: DeckReviewPassSuccessMark,
)