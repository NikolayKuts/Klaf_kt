package com.example.klaf.domain.useCases

import com.example.klaf.di.CardRepositoryRoomImp
import com.example.klaf.di.DeckRepositoryRoomImp
import com.example.klaf.di.StorageSaveVersionRepositoryRoomImp
import com.example.klaf.domain.entities.Card
import com.example.klaf.domain.entities.Deck
import com.example.klaf.domain.repositories.CardRepository
import com.example.klaf.domain.repositories.DeckRepository
import com.example.klaf.domain.repositories.StorageSaveVersionRepository
import com.example.klaf.domain.repositories.StorageTransactionRepository
import kotlinx.coroutines.*
import javax.inject.Inject

class MoveCarsToDeckUseCase @Inject constructor(
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