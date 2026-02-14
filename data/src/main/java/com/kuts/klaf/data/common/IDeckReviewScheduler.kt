package com.kuts.klaf.data.common

interface IDeckReviewScheduler {

    fun schedule(
        deckName: String,
        deckId: Int,
        atTime: Long,
    )
}
