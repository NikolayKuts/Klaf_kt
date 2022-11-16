package com.example.klaf.domain.useCases

import com.example.klaf.di.CardRepositoryRoomImp
import com.example.klaf.di.DeckRepositoryRoomImp
import com.example.klaf.di.StorageSaveVersionRepositoryRoomImp
import com.example.klaf.domain.repositories.CardRepository
import com.example.klaf.domain.repositories.DeckRepository
import com.example.klaf.domain.repositories.StorageSaveVersionRepository
import com.example.klaf.domain.repositories.StorageTransactionRepository
import com.example.klaf.presentation.common.log
import kotlinx.coroutines.*
import javax.inject.Inject

class RemoveDeckUseCase @Inject constructor(
    @DeckRepositoryRoomImp
    private val deckRepository: DeckRepository,
    @CardRepositoryRoomImp
    private val cardRepository: CardRepository,
    @StorageSaveVersionRepositoryRoomImp
    private val localStorageSaveVersionRepository: StorageSaveVersionRepository,
    private val localStorageTransactionRepository: StorageTransactionRepository,
) {

    suspend operator fun invoke(deckId: Int) {
        withContext(Dispatchers.IO) {
            localStorageTransactionRepository.performWithTransaction {
                coroutineScope {
                    val deckDeletingJob = launch() {
                        deckRepository.removeDeck(deckId = deckId)
                    }
                    val cardDeletingJob = launch() {
                        cardRepository.removeCardsOfDeck(deckId = deckId)
                    }

                    joinAll(deckDeletingJob, cardDeletingJob)
                    localStorageSaveVersionRepository.increaseVersion()
                }
            }
        }
    }
}