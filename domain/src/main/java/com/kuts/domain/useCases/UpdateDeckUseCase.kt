package com.kuts.domain.useCases

import com.kuts.domain.common.LocalDeckRepositoryImp
import com.kuts.domain.common.LocalStorageSaveVersionRepositoryImp
import com.kuts.domain.entities.Deck
import com.kuts.domain.repositories.DeckRepository
import com.kuts.domain.repositories.StorageSaveVersionRepository
import com.kuts.domain.repositories.StorageTransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UpdateDeckUseCase @Inject constructor(
    @LocalDeckRepositoryImp
    private val deckRepository: DeckRepository,
    @LocalStorageSaveVersionRepositoryImp
    private val localStorageSaveVersionRepository: StorageSaveVersionRepository,
    private val localStorageTransactionRepository: StorageTransactionRepository,
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