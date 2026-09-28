package com.kuts.domain.useCases

import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.entities.Deck
import com.kuts.domain.entities.DeckRepetitionInfo
import com.kuts.domain.repositories.IDeckReviewResultRepository
import kotlinx.coroutines.withContext

class SaveCompletedDeckReviewUseCase(
    private val repository: IDeckReviewResultRepository,
    private val coroutineContextProvider: ICoroutineContextProvider,
) {

    suspend operator fun invoke(updatedDeck: Deck, reviewInfo: DeckRepetitionInfo) {
        withContext(coroutineContextProvider.io) {
            repository.save(updatedDeck, reviewInfo)
        }
    }
}
