package com.kuts.domain.useCases

import com.kuts.domain.common.LocalCardRepository
import com.kuts.domain.common.LocalDeckRepository
import com.kuts.domain.common.LocalStorageSaveVersionRepository
import com.kuts.domain.repositories.ICardRepository
import com.kuts.domain.repositories.IDeckRepository
import com.kuts.domain.repositories.IStorageSaveVersionRepository
import com.kuts.domain.repositories.IStorageTransactionRepository
import kotlinx.coroutines.*
import javax.inject.Inject

class DeleteCardsFromDeckUseCase @Inject constructor(
    @LocalDeckRepository
    private val deckRepository: IDeckRepository,
    @LocalCardRepository
    private val cardRepository: ICardRepository,
    @LocalStorageSaveVersionRepository
    private val localStorageSaveVersionRepository: IStorageSaveVersionRepository,
    private val localStorageTransactionRepository: IStorageTransactionRepository,
) {

    suspend operator fun invoke(deckId: Int, vararg cardIds: Int) {
        withContext(Dispatchers.IO) {
            localStorageTransactionRepository.performWithTransaction {
                val originDeck = deckRepository.getDeckById(deckId = deckId)
                    ?: throw Exception("Fetching deck is failed")

                coroutineScope {
                    val deletingJobs = mutableSetOf<Job>()

                    cardIds.onEach { id ->
                        launch { cardRepository.deleteCard(cardId = id) }
                            .also { job -> deletingJobs.add(job) }
                    }

                    joinAll(jobs = deletingJobs.toTypedArray())
                }

                val actualCardQuantityInDeck =
                    cardRepository.fetchCardQuantityByDeckId(deckId = deckId)
                val updatedDeck = originDeck.copy(cardQuantity = actualCardQuantityInDeck)

                deckRepository.insertDeck(deck = updatedDeck)
                localStorageSaveVersionRepository.increaseVersion()
            }
        }
    }
}