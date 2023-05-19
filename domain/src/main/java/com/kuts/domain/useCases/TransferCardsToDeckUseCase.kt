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
import kotlinx.coroutines.*
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

    suspend operator fun invoke(sourceDeck: Deck, targetDeck: Deck, vararg cards: Card) {
        withContext(Dispatchers.IO) {
            localStorageTransactionRepository.performWithTransaction {
                coroutineScope {
                    cards.map { card ->
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