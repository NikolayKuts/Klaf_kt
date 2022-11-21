package com.example.klaf.domain.useCases

import com.example.klaf.di.CardRepositoryRoomImp
import com.example.klaf.di.DeckRepositoryRoomImp
import com.example.klaf.di.StorageSaveVersionRepositoryRoomImp
import com.example.klaf.domain.repositories.*
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
    private val deckRepetitionInfoRepository: DeckRepetitionInfoRepository
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