package com.example.klaf.domain.entities

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
    val currentIterationSuccessMark: DeckRepetitionSuccessMark,
    val previousIterationSuccessMark: DeckRepetitionSuccessMark,
)