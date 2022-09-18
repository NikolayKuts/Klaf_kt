package com.example.klaf.domain.useCases

import com.example.klaf.di.DeckRepositoryRoomImp
import com.example.klaf.di.StorageSaveVersionRepositoryRoomImp
import com.example.klaf.domain.entities.Deck
import com.example.klaf.domain.repositories.DeckRepository
import com.example.klaf.domain.repositories.StorageSaveVersionRepository
import com.example.klaf.domain.repositories.StorageTransactionRepository
import com.example.klaf.presentation.common.log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UpdateDeckUseCase @Inject constructor(
    @DeckRepositoryRoomImp
    private val deckRepository: DeckRepository,
    @StorageSaveVersionRepositoryRoomImp
    private val localStorageSaveVersionRepository: StorageSaveVersionRepository,
    private val localStorageTransactionRepository: StorageTransactionRepository
) {

    suspend operator fun invoke(updatedDeck: Deck) {
        withContext(Dispatchers.IO) {
            localStorageTransactionRepository.performWithTransaction {
                deckRepository.insertDeck(deck = updatedDeck)
                localStorageSaveVersionRepository.increaseVersion()
            }
        }
    }
}