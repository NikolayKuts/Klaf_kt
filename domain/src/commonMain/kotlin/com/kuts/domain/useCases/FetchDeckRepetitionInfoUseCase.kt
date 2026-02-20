package com.kuts.domain.useCases

import com.kuts.domain.entities.DeckRepetitionInfo
import com.kuts.domain.repositories.IDeckRepetitionInfoRepository
import kotlinx.coroutines.flow.Flow

class FetchDeckRepetitionInfoUseCase(
    private val deckRepetitionInfoRepository: IDeckRepetitionInfoRepository,
) {

    operator fun invoke(deckId: Int): Flow<DeckRepetitionInfo?> {
        return deckRepetitionInfoRepository.fetchDeckRepetitionInfo(deckId = deckId)
    }
}