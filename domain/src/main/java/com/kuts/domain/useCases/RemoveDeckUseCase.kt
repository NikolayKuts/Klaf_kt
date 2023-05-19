package com.kuts.domain.useCases

import com.kuts.domain.common.LocalCardRepositoryImp
import com.kuts.domain.common.LocalDeckRepositoryImp
import com.kuts.domain.common.LocalStorageSaveVersionRepositoryImp
import com.kuts.domain.repositories.*
import kotlinx.coroutines.*
import javax.inject.Inject

class RemoveDeckUseCase @Inject constructor(
    @LocalDeckRepositoryImp
    private val deckRepository: DeckRepository,
    @LocalCardRepositoryImp
    private val cardRepository: CardRepository,
    @LocalStorageSaveVersionRepositoryImp
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