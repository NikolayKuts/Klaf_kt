package com.kuts.domain.useCases

import com.kuts.domain.entities.Deck
import com.kuts.domain.repositories.IDeckRepository
import com.kuts.domain.repositories.IStorageSaveVersionRepository
import com.kuts.domain.repositories.IStorageTransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CreateDeckUseCase(
    private val deckRepository: IDeckRepository,
    private val localStorageSaveVersionRepository: IStorageSaveVersionRepository,
    private val localStorageTransactionRepository: IStorageTransactionRepository,
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