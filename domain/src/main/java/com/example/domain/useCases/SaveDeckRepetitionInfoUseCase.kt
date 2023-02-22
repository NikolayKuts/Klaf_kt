package com.example.domain.useCases

import com.example.domain.entities.DeckRepetitionInfo
import com.example.domain.repositories.DeckRepetitionInfoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SaveDeckRepetitionInfoUseCase @Inject constructor(
    private val deckRepetitionInfoRepository: DeckRepetitionInfoRepository,
) {

    suspend operator fun invoke(deckRepetitionInfo: DeckRepetitionInfo) {
        withContext(Dispatchers.IO) {
            deckRepetitionInfoRepository.saveDeckRepetitionInfo(info = deckRepetitionInfo)
        }
    }
}