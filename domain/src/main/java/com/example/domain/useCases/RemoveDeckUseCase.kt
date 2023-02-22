package com.example.domain.useCases

import com.example.domain.common.CardRepositoryRoomImp
import com.example.domain.common.DeckRepositoryRoomImp
import com.example.domain.common.StorageSaveVersionRepositoryRoomImp
import com.example.domain.repositories.*
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
    private val deckRepetitionInfoRepository: DeckRepetitionInfoRepository,
) {

    suspend operator fun invoke(deckId: Int) {
        withContext(Dispatchers.IO) {
            localStorageTransactionRepository.performWithTransaction {
                coroutineScope {
                    val deckDeletingJob = launch {
                        deckRepository.removeDeck(deckId = deckId)
                    }
                    val cardDeletingJob = launch {
                        cardRepository.removeCardsOfDeck(deckId = deckId)
                    }
                    val deckRepetitionInfoDeleting = launch {
                        deckRepetitionInfoRepository.removeDeckRepetitionInfo(deckId = deckId)
                    }

                    joinAll(deckDeletingJob, cardDeletingJob, deckRepetitionInfoDeleting)
                    localStorageSaveVersionRepository.increaseVersion()
                }
            }
        }
    }
}