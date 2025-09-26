package com.kuts.domain.useCases

import com.kuts.domain.common.LocalCardRepositoryImp
import com.kuts.domain.common.LocalDeckRepositoryImp
import com.kuts.domain.common.LocalStorageSaveVersionRepositoryImp
import com.kuts.domain.entities.Card
import com.kuts.domain.entities.Deck
import com.kuts.domain.repositories.CardRepository
import com.kuts.domain.repositories.DeckRepository
import com.kuts.domain.repositories.StorageSaveVersionRepository
import com.kuts.domain.repositories.StorageTransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class TransferCardsToDeckUseCase @Inject constructor(
    @LocalCardRepositoryImp
    private val cardRepository: CardRepository,
    @LocalDeckRepositoryImp
    private val deckRepository: DeckRepository,
    @LocalStorageSaveVersionRepositoryImp
    private val localStorageSaveVersionRepository: StorageSaveVersionRepository,
    private val localStorageTransactionRepository: StorageTransactionRepository,
) {

    suspend operator fun invoke(sourceDeck: Deck, targetDeck: Deck, vararg cardsToMove: Card) {
        withContext(Dispatchers.IO) {
            localStorageTransactionRepository.performWithTransaction {
                val sourceDurationPerCard = sourceDeck.sourceDurationPerCard()
                val targetDurationPerCard = targetDeck.sourceDurationPerCard()

                val updatedSourceIterationDuration =
                    (sourceDeck.lastReviewPassDuration - sourceDurationPerCard * cardsToMove.size)
                        .coerceAtLeast(0L)


                val updatedTargetIterationDuration = if (targetDeck.cardQuantity > 0) {
                    (targetDeck.lastReviewPassDuration + targetDurationPerCard * cardsToMove.size)
                        .coerceAtLeast(0L)
                } else {
                    (sourceDurationPerCard * cardsToMove.size).coerceAtLeast(0L)
                }

                coroutineScope {
                    cardsToMove.map { card ->
                        card.id to Card(
                            deckId = targetDeck.id,
                            nativeWord = card.nativeWord,
                            foreignWord = card.foreignWord,
                            ipa = card.ipa,
                        )
                    }.onEach { (oldId, updatedCard) ->
                        launch { cardRepository.deleteCard(cardId = oldId) }
                        launch { cardRepository.insertCard(card = updatedCard) }
                    }
                }

                sourceDeck.lastFirstReviewDuration

                updateDeck(
                    deck = sourceDeck,
                    iterationDuration = updatedSourceIterationDuration,
                )
                updateDeck(
                    deck = targetDeck,
                    iterationDuration = updatedTargetIterationDuration,
                )

                updateDeckReviewInfo(sourceDeck)
                updateDeckReviewInfo(targetDeck)

                localStorageSaveVersionRepository.increaseVersion()
            }
        }
    }

    private suspend fun updateDeck(
        deck: Deck,
        iterationDuration: Long,
    ) {
        val updatedDeck = deck.copy(
            cardQuantity = cardRepository.fetchCardQuantityByDeckId(deckId = deck.id),
            lastReviewPassDuration = iterationDuration,
        )

        deckRepository.insertDeck(deck = updatedDeck)
    }

    private fun Deck.sourceDurationPerCard(): Long {
        return if (cardQuantity > 0) {
            lastReviewPassDuration / cardQuantity
        } else {
            0L
        }
    }

    private fun updateDeckReviewInfo(deck: Deck) {
       // TODO (implement updating deck review info)
    }
}