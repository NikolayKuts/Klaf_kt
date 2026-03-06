package com.kuts.klaf.deckRepetition

import kotlinx.serialization.Serializable

@Serializable
data class DeckReviewState(
    val reviewedCardsCount: Int = 0,
    val leftTime: Long = 0L,
    val maxTime: Long = 0L,
)
