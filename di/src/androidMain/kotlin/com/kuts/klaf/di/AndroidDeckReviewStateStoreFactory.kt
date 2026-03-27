package com.kuts.klaf.di

import androidx.lifecycle.SavedStateHandle
import com.kuts.klaf.deckRepetition.IDeckReviewStateStore
import com.kuts.klaf.deckRepetition.savedStateHandle.DeckReviewSavedStateHandleStateStore

internal class AndroidDeckReviewStateStoreFactory(
    private val handle: SavedStateHandle,
) : IDeckReviewStateStoreFactory {

    override fun create(): IDeckReviewStateStore {
        return DeckReviewSavedStateHandleStateStore(handle = handle)
    }
}
