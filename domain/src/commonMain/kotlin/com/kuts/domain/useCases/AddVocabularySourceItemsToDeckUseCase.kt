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
import com.kuts.domain.repositories.IIgnoredVocabularyWordRepository
import com.kuts.domain.vocabularySource.reviewKey
import com.kuts.domain.vocabularySource.toVocabularyKey
import com.kuts.domain.vocabularySource.toMeaningKey
import kotlinx.coroutines.withContext

class AddVocabularySourceItemsToDeckUseCase(
    private val cardRepository: ICardRepository,
    private val deckRepository: IDeckRepository,
    private val vocabularySourceRepository: IVocabularySourceRepository,
    private val localStorageSaveVersionRepository: IStorageSaveVersionRepository,
    private val localStorageTransactionRepository: IStorageTransactionRepository,
    private val coroutineContextProvider: ICoroutineContextProvider,
    private val ignoredVocabularyWordRepository: IIgnoredVocabularyWordRepository,
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
                val existingPairs = cardRepository.fetchAllCards().map {
                    it.foreignWord.toVocabularyKey() to it.nativeWord.toMeaningKey()
                }.toSet()
                val ignoredKeys = ignoredVocabularyWordRepository.fetchWords().map { it.reviewKey() }.toSet()
                if (items.none {
                    it.status == VocabularySourceItemStatus.PENDING && !it.alreadyExists &&
                        it.reviewKey() !in ignoredKeys &&
                        (it.foreignWord.toVocabularyKey() to it.nativeWord.toMeaningKey()) !in existingPairs
                }) return@performWithTransaction 0
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
        val existingPairs = cardRepository.fetchAllCards().map {
            it.foreignWord.toVocabularyKey() to it.nativeWord.toMeaningKey()
        }.toMutableSet()
        val ignoredKeys = ignoredVocabularyWordRepository.fetchWords().map { it.reviewKey() }.toSet()
        val addedItems = items
            .filter { item ->
                item.status == VocabularySourceItemStatus.PENDING && !item.alreadyExists
            }
            .mapNotNull { item ->
                val pair = item.foreignWord.toVocabularyKey() to item.nativeWord.toMeaningKey()
                if (item.reviewKey() in ignoredKeys || !existingPairs.add(pair)) return@mapNotNull null
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
