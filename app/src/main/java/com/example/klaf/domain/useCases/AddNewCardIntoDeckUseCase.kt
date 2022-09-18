package com.example.klaf.domain.useCases

import com.example.klaf.di.CardRepositoryRoomImp
import com.example.klaf.di.DeckRepositoryRoomImp
import com.example.klaf.di.StorageSaveVersionRepositoryRoomImp
import com.example.klaf.domain.entities.Card
import com.example.klaf.domain.repositories.CardRepository
import com.example.klaf.domain.repositories.DeckRepository
import com.example.klaf.domain.repositories.StorageSaveVersionRepository
import com.example.klaf.domain.repositories.StorageTransactionRepository
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
    private val localStorageTransactionRepository: StorageTransactionRepository
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

