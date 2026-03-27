package com.kuts.klaf.di

import com.kuts.klaf.desktop.DesktopInMemoryDeckReviewStateStore
import com.kuts.klaf.deckRepetition.IDeckReviewStateStore

internal class DesktopDeckReviewStateStoreFactory : IDeckReviewStateStoreFactory {

    override fun create(): IDeckReviewStateStore {
        return DesktopInMemoryDeckReviewStateStore()
    }
}
