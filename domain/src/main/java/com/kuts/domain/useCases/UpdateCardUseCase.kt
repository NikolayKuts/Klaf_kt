package com.kuts.domain.useCases

import com.kuts.domain.entities.Card
import com.kuts.domain.repositories.ICardRepository
import com.kuts.domain.repositories.IStorageSaveVersionRepository
import com.kuts.domain.repositories.IStorageTransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UpdateCardUseCase(
    private val cardRepository: ICardRepository,
    private val localStorageSaveVersionRepository: IStorageSaveVersionRepository,
    private val localStorageTransactionRepository: IStorageTransactionRepository,
) {

    suspend operator fun invoke(newCard: Card) {
        withContext(Dispatchers.IO) {
            localStorageTransactionRepository.performWithTransaction {
                cardRepository.insertCard(card = newCard)
                localStorageSaveVersionRepository.increaseVersion()
            }
        }
    }
}