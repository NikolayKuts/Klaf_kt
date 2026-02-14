package com.kuts.domain.useCases

import com.kuts.domain.entities.Deck
import com.kuts.domain.repositories.IDeckRepository
import com.kuts.domain.repositories.IStorageSaveVersionRepository
import com.kuts.domain.repositories.IStorageTransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UpdateDeckUseCase(
    private val deckRepository: IDeckRepository,
    private val localStorageSaveVersionRepository: IStorageSaveVersionRepository,
    private val localStorageTransactionRepository: IStorageTransactionRepository,
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