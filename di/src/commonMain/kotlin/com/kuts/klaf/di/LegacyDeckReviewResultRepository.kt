package com.kuts.klaf.di

import com.kuts.domain.entities.Deck
import com.kuts.domain.entities.DeckRepetitionInfo
import com.kuts.domain.repositories.IDeckReviewResultRepository
import com.kuts.domain.useCases.SaveDeckReviewInfoUseCase
import com.kuts.domain.useCases.UpdateDeckUseCase

/** Keeps the installed legacy save path until the account-scoped Room cutover. */
internal class LegacyDeckReviewResultRepository(
    private val updateDeck: UpdateDeckUseCase,
    private val saveDeckReviewInfo: SaveDeckReviewInfoUseCase,
) : IDeckReviewResultRepository {

    override suspend fun save(updatedDeck: Deck, reviewInfo: DeckRepetitionInfo) {
        updateDeck(updatedDeck)
        saveDeckReviewInfo(reviewInfo)
    }
}
