package com.kuts.klaf.common

interface IDeckReviewScheduler {

    fun schedule(
        deckName: String,
        deckId: Int,
        atTime: Long,
    )
}
