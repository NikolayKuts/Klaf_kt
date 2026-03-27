package com.kuts.klaf.di

import com.kuts.klaf.deckRepetition.IDeckReviewStateStore
import com.kuts.klaf.ios.IosInMemoryDeckReviewStateStore

internal class IosDeckReviewStateStoreFactory : IDeckReviewStateStoreFactory {

    override fun create(): IDeckReviewStateStore {
        return IosInMemoryDeckReviewStateStore()
    }
}
