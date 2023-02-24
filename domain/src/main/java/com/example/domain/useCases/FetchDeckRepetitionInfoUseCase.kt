package com.example.domain.useCases

import com.example.domain.entities.DeckRepetitionInfo
import com.example.domain.repositories.DeckRepetitionInfoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FetchDeckRepetitionInfoUseCase @Inject constructor(
    private val deckRepetitionInfoRepository: DeckRepetitionInfoRepository,
) {

    operator fun invoke(deckId: Int): Flow<DeckRepetitionInfo?> {
        return deckRepetitionInfoRepository.fetchDeckRepetitionInfo(deckId = deckId)
    }
}