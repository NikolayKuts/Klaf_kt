package com.kuts.domain.useCases

import com.kuts.domain.repositories.*
import kotlinx.coroutines.*

class RemoveDeckUseCase(
    private val deckRepository: IDeckRepository,
    private val cardRepository: ICardRepository,
    private val localStorageSaveVersionRepository: IStorageSaveVersionRepository,
    private val localStorageTransactionRepository: IStorageTransactionRepository,
    private val deckRepetitionInfoRepository: IDeckRepetitionInfoRepository,
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