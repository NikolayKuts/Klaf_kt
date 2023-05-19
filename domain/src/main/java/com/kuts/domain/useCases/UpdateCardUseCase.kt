package com.kuts.domain.useCases

import com.kuts.domain.common.LocalCardRepositoryImp
import com.kuts.domain.common.LocalStorageSaveVersionRepositoryImp
import com.kuts.domain.entities.Card
import com.kuts.domain.repositories.CardRepository
import com.kuts.domain.repositories.StorageSaveVersionRepository
import com.kuts.domain.repositories.StorageTransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UpdateCardUseCase @Inject constructor(
    @LocalCardRepositoryImp
    private val cardRepository: CardRepository,
    @LocalStorageSaveVersionRepositoryImp
    private val localStorageSaveVersionRepository: StorageSaveVersionRepository,
    private val localStorageTransactionRepository: StorageTransactionRepository,
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