package com.example.klaf.domain.useCases

import com.example.klaf.data.dataStore.DeckRepetitionInfo
import com.example.klaf.domain.repositories.DeckRepetitionInfoRepository
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