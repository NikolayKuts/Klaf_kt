package com.kuts.domain.useCases

import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.common.ReviewedDeckCardAdditionException
import com.kuts.domain.common.getCurrentDateAsLong
import com.kuts.domain.entities.Card
import com.kuts.domain.entities.Deck
import com.kuts.domain.entities.VocabularySourceItem
import com.kuts.domain.entities.VocabularySourceItemStatus
import com.kuts.domain.repositories.ICardRepository
import com.kuts.domain.repositories.IDeckRepository
import com.kuts.domain.repositories.IStorageSaveVersionRepository
import com.kuts.domain.repositories.IStorageTransactionRepository
import com.kuts.domain.repositories.IVocabularySourceRepository
import kotlinx.coroutines.withContext

class AddVocabularySourceItemsToDeckUseCase(
    private val cardRepository: ICardRepository,
    private val deckRepository: IDeckRepository,
    private val vocabularySourceRepository: IVocabularySourceRepository,
    private val localStorageSaveVersionRepository: IStorageSaveVersionRepository,
    private val localStorageTransactionRepository: IStorageTransactionRepository,
    private val coroutineContextProvider: ICoroutineContextProvider,
) {

    suspend operator fun invoke(deck: Deck, items: List<VocabularySourceItem>): Int {
        return withContext(context = coroutineContextProvider.io) {
            localStorageTransactionRepository.performWithTransaction {
                val currentTime = getCurrentDateAsLong()
                addItemsToDeck(deck = deck, items = items, currentTime = currentTime)
            }
        }
    }

    suspend operator fun invoke(deckName: String, items: List<VocabularySourceItem>): Int {
        return withContext(context = coroutineContextProvider.io) {
            localStorageTransactionRepository.performWithTransaction {
                val currentTime = getCurrentDateAsLong()
                val deckId = deckRepository.insertDeck(
                    deck = Deck(
                        name = deckName,
                        creationDate = currentTime,
                    )
                )

                addItemsToDeck(
                    deck = Deck(
                        id = deckId,
                        name = deckName,
                        creationDate = currentTime,
                    ),
                    items = items,
                    currentTime = currentTime,
                )
            }
        }
    }

    private suspend fun addItemsToDeck(
        deck: Deck,
        items: List<VocabularySourceItem>,
        currentTime: Long,
    ): Int {
        val currentDeck = deckRepository.getDeckById(deck.id)
            ?: error("Destination deck no longer exists")
        if (currentDeck.reviewCount != 0) throw ReviewedDeckCardAdditionException()
        val addedItems = items
            .filter { item ->
                item.status == VocabularySourceItemStatus.PENDING && !item.alreadyExists
            }
            .map { item ->
                val cardId = cardRepository.insertCard(
                    card = Card(
                        deckId = deck.id,
                        nativeWord = item.nativeWord,
                        foreignWord = item.foreignWord,
                        ipa = emptyList(),
                    )
                )

                item.copy(
                    status = VocabularySourceItemStatus.ADDED,
                    createdCardId = cardId,
                    targetDeckId = deck.id,
                    updatedAt = currentTime,
                )
            }

        if (addedItems.isNotEmpty()) {
            vocabularySourceRepository.saveItems(items = addedItems)
            deckRepository.insertDeck(
                deck = currentDeck.copy(
                    cardQuantity = cardRepository.fetchCardQuantityByDeckId(deckId = deck.id),
                )
            )
            localStorageSaveVersionRepository.increaseVersion()
        }

        return addedItems.size
    }
}
