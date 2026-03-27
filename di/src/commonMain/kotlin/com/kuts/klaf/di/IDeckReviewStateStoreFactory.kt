package com.kuts.klaf.di

import com.kuts.klaf.deckRepetition.IDeckReviewStateStore

internal interface IDeckReviewStateStoreFactory {

    fun create(): IDeckReviewStateStore
}
