package com.example.domain.useCases

import com.example.domain.common.LocalDeckRepositoryImp
import com.example.domain.common.LocalStorageSaveVersionRepositoryImp
import com.example.domain.entities.Deck
import com.example.domain.repositories.DeckRepository
import com.example.domain.repositories.StorageSaveVersionRepository
import com.example.domain.repositories.StorageTransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CreateDeckUseCase @Inject constructor(
    @LocalDeckRepositoryImp
    private val deckRepository: DeckRepository,
    @LocalStorageSaveVersionRepositoryImp
    private val localStorageSaveVersionRepository: StorageSaveVersionRepository,
    private val localStorageTransactionRepository: StorageTransactionRepository,
) {

    suspend operator fun invoke(deck: Deck) {
        withContext(Dispatchers.IO) {
            localStorageTransactionRepository.performWithTransaction {
                deckRepository.insertDeck(deck = deck)
                localStorageSaveVersionRepository.increaseVersion()
            }
        }
    }
}