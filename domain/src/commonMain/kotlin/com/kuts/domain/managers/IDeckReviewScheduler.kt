package com.kuts.domain.managers

interface IDeckReviewScheduler {

    fun schedule(
        deckName: String,
        deckId: Int,
        atTime: Long = 0L,
    )
}
