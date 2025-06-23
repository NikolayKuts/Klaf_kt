package com.kuts.klaf.data.common

interface DeckReviewScheduler {

    fun schedule(
        deckName: String,
        deckId: Int,
        atTime: Long = 0,
    )
}