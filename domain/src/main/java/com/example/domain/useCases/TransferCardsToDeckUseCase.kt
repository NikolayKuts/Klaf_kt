package com.example.domain.useCases

import com.example.domain.common.CardRepositoryRoomImp
import com.example.domain.common.DeckRepositoryRoomImp
import com.example.domain.common.StorageSaveVersionRepositoryRoomImp
import com.example.domain.entities.Card
import com.example.domain.entities.Deck
import com.example.domain.repositories.CardRepository
import com.example.domain.repositories.DeckRepository
import com.example.domain.repositories.StorageSaveVersionRepository
import com.example.domain.repositories.StorageTransactionRepository
import kotlinx.coroutines.*
import javax.inject.Inject

class TransferCardsToDeckUseCase @Inject constructor(
    @CardRepositoryRoomImp
    private val cardRepository: CardRepository,
    @DeckRepositoryRoomImp
    private val deckRepository: DeckRepository,
    @StorageSaveVersionRepositoryRoomImp
    private val localStorageSaveVersionRepository: StorageSaveVersionRepository,
    private val localStorageTransactionRepository: StorageTransactionRepository,
) {

    suspend operator fun invoke(sourceDeck: Deck, targetDeck: Deck, vararg cards: Card) {
        withContext(Dispatchers.IO) {
            localStorageTransactionRepository.performWithTransaction {
                coroutineScope {
                    val updatingJobs = mutableSetOf<Job>()

                    cards.map { card -> card.copy(deckId = targetDeck.id) }
                        .onEach { updatedCard ->
                            launch { cardRepository.insertCard(card = updatedCard) }
                                .also { job -> updatingJobs.add(element = job) }
                        }

                    joinAll(jobs = updatingJobs.toTypedArray())
                }

                updateCardQuantity(deck = sourceDeck)
                updateCardQuantity(deck = targetDeck)
                localStorageSaveVersionRepository.increaseVersion()
            }
        }
    }

    private suspend fun updateCardQuantity(deck: Deck) {
        val updatedDeck = deck.copy(
            cardQuantity = cardRepository.fetchCardQuantityByDeckId(deckId = deck.id)
        )

        deckRepository.insertDeck(deck = updatedDeck)
    }
}