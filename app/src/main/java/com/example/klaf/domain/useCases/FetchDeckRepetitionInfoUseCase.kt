package com.example.klaf.domain.useCases

import com.example.klaf.domain.entities.DeckRepetitionInfo
import com.example.klaf.domain.repositories.DeckRepetitionInfoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FetchDeckRepetitionInfoUseCase @Inject constructor(
    private val deckRepetitionInfoRepository: DeckRepetitionInfoRepository,
) {

    operator fun invoke(deckId: Int): Flow<DeckRepetitionInfo?> {
        return deckRepetitionInfoRepository.fetchDeckRepetitionInfo(deckId = deckId)
    }
}