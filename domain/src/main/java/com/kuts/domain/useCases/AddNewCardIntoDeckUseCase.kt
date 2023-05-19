package com.kuts.domain.useCases

import com.kuts.domain.common.LocalCardRepositoryImp
import com.kuts.domain.common.LocalDeckRepositoryImp
import com.kuts.domain.common.LocalStorageSaveVersionRepositoryImp
import com.kuts.domain.entities.Card
import com.kuts.domain.repositories.CardRepository
import com.kuts.domain.repositories.DeckRepository
import com.kuts.domain.repositories.StorageSaveVersionRepository
import com.kuts.domain.repositories.StorageTransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AddNewCardIntoDeckUseCase @Inject constructor(
    @LocalDeckRepositoryImp
    private val deckRepository: DeckRepository,
    @LocalCardRepositoryImp
    private val cardRepository: CardRepository,
    @LocalStorageSaveVersionRepositoryImp
    private val localStorageSaveVersionRepository: StorageSaveVersionRepository,
    private val localStorageTransactionRepository: StorageTransactionRepository,
) {

    suspend operator fun invoke(card: Card) {
        withContext(Dispatchers.IO) {
            localStorageTransactionRepository.performWithTransaction {
                val originalDeck = deckRepository.getDeckById(deckId = card.deckId)
                    ?: throw Exception("Fetching deck is failed")

                cardRepository.insertCard(card = card)
                val actualCardQuantity =
                    cardRepository.fetchCardQuantityByDeckId(deckId = card.deckId)
                val updatedDeck = originalDeck.copy(cardQuantity = actualCardQuantity)

                deckRepository.insertDeck(deck = updatedDeck)
                localStorageSaveVersionRepository.increaseVersion()
            }
        }
    }
}

