package com.kuts.domain.useCases

import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.entities.DeckRepetitionInfo
import com.kuts.domain.repositories.IDeckRepetitionInfoRepository
import kotlinx.coroutines.withContext

class SaveDeckReviewInfoUseCase(
    private val deckRepetitionInfoRepository: IDeckRepetitionInfoRepository,
    private val coroutineContextProvider: ICoroutineContextProvider,
) {

    suspend operator fun invoke(deckRepetitionInfo: DeckRepetitionInfo) {
        withContext(context = coroutineContextProvider.io) {
            deckRepetitionInfoRepository.saveDeckRepetitionInfo(info = deckRepetitionInfo)
        }
    }
}
