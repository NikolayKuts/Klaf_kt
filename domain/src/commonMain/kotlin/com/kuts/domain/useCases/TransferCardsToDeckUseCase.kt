package com.kuts.domain.useCases

import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.entities.Card
import com.kuts.domain.entities.Deck
import com.kuts.domain.repositories.ICardRepository
import com.kuts.domain.repositories.IDeckRepository
import com.kuts.domain.repositories.IStorageSaveVersionRepository
import com.kuts.domain.repositories.IStorageTransactionRepository
import kotlinx.coroutines.withContext

class TransferCardsToDeckUseCase(
    private val cardRepository: ICardRepository,
    private val deckRepository: IDeckRepository,
    private val localStorageSaveVersionRepository: IStorageSaveVersionRepository,
    private val localStorageTransactionRepository: IStorageTransactionRepository,
    private val coroutineContextProvider: ICoroutineContextProvider,
) {

    suspend operator fun invoke(sourceDeck: Deck, targetDeck: Deck, vararg cardsToMove: Card) {
        withContext(context = coroutineContextProvider.io) {
            localStorageTransactionRepository.performWithTransaction {
                require(sourceDeck.id != targetDeck.id) { "Source and destination decks must differ" }
                require(cardsToMove.isNotEmpty()) { "No cards selected for transfer" }
                require(cardsToMove.map(Card::id).distinct().size == cardsToMove.size) {
                    "A card cannot be transferred twice in one operation"
                }

                val currentSource = requireNotNull(deckRepository.getDeckById(sourceDeck.id)) {
                    "Source deck no longer exists"
                }
                val currentTarget = requireNotNull(deckRepository.getDeckById(targetDeck.id)) {
                    "Destination deck no longer exists"
                }
                check(currentTarget.reviewCount == 0) { "Cannot move cards into a reviewed deck" }

                val currentCardsById = cardRepository.fetchCardsByDeckId(currentSource.id).associateBy(Card::id)
                val cards = cardsToMove.map { selected ->
                    requireNotNull(currentCardsById[selected.id]) { "Card ${selected.id} is no longer in the source deck" }
                }

                val sourceDurationPerCard = currentSource.sourceDurationPerCard()
                val targetDurationPerCard = currentTarget.sourceDurationPerCard()

                val updatedSourceIterationDuration = (
                    currentSource.lastReviewPassDuration - sourceDurationPerCard * cards.size
                ).coerceAtLeast(0L)

                val updatedTargetIterationDuration = if (currentTarget.cardQuantity > 0) {
                    (currentTarget.lastReviewPassDuration + targetDurationPerCard * cards.size)
                        .coerceAtLeast(0L)
                } else {
                    (sourceDurationPerCard * cards.size).coerceAtLeast(0L)
                }

                cards.forEach { card ->
                    cardRepository.insertCard(card = card.copy(deckId = currentTarget.id))
                }

                updateDeck(
                    deck = currentSource,
                    iterationDuration = updatedSourceIterationDuration,
                )
                updateDeck(
                    deck = currentTarget,
                    iterationDuration = updatedTargetIterationDuration,
                )

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
}
