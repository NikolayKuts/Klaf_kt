package com.example.domain.useCases

import com.example.domain.common.CardRepositoryRoomImp
import com.example.domain.common.DeckRepositoryRoomImp
import com.example.domain.common.StorageSaveVersionRepositoryRoomImp
import com.example.domain.entities.Card
import com.example.domain.repositories.CardRepository
import com.example.domain.repositories.DeckRepository
import com.example.domain.repositories.StorageSaveVersionRepository
import com.example.domain.repositories.StorageTransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AddNewCardIntoDeckUseCase @Inject constructor(
    @DeckRepositoryRoomImp
    private val deckRepository: DeckRepository,
    @CardRepositoryRoomImp
    private val cardRepository: CardRepository,
    @StorageSaveVersionRepositoryRoomImp
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

