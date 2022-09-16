package com.example.klaf.domain.useCases

import com.example.klaf.di.DeckRepositoryRoomImp
import com.example.klaf.di.StorageSaveVersionRepositoryRoomImp
import com.example.klaf.domain.repositories.DeckRepository
import com.example.klaf.domain.repositories.StorageSaveVersionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class RemoveDeckUseCase @Inject constructor(
    @DeckRepositoryRoomImp
    private val deckRepository: DeckRepository,
    @StorageSaveVersionRepositoryRoomImp
    private val localStorageSaveVersionRepository: StorageSaveVersionRepository
) {

    suspend operator fun invoke(deckId: Int) {
        withContext(Dispatchers.IO) {
            deckRepository.removeDeck(deckId = deckId)
            localStorageSaveVersionRepository.increaseVersion()
        }
    }
}