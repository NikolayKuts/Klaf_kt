package com.kuts.domain.useCases

import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.repositories.*
import kotlinx.coroutines.withContext

class RemoveDeckUseCase(
    private val deckRepository: IDeckRepository,
    private val cardRepository: ICardRepository,
    private val localStorageSaveVersionRepository: IStorageSaveVersionRepository,
    private val localStorageTransactionRepository: IStorageTransactionRepository,
    private val deckRepetitionInfoRepository: IDeckRepetitionInfoRepository,
    private val coroutineContextProvider: ICoroutineContextProvider,
) {

    suspend operator fun invoke(deckId: Int) {
        withContext(context = coroutineContextProvider.io) {
            localStorageTransactionRepository.performWithTransaction {
                deckRepository.removeDeck(deckId = deckId)
                cardRepository.removeCardsOfDeck(deckId = deckId)
                deckRepetitionInfoRepository.removeDeckRepetitionInfo(deckId = deckId)
                localStorageSaveVersionRepository.increaseVersion()
            }
        }
    }
}
