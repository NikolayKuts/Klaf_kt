package com.example.domain.useCases

import com.example.domain.common.CardRepositoryRoomImp
import com.example.domain.common.StorageSaveVersionRepositoryRoomImp
import com.example.domain.entities.Card
import com.example.domain.repositories.CardRepository
import com.example.domain.repositories.StorageSaveVersionRepository
import com.example.domain.repositories.StorageTransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UpdateCardUseCase @Inject constructor(
    @CardRepositoryRoomImp
    private val cardRepository: CardRepository,
    @StorageSaveVersionRepositoryRoomImp
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