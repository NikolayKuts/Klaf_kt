package com.example.klaf.domain.useCases

import com.example.klaf.di.CardRepositoryRoomImp
import com.example.klaf.di.DeckRepositoryRoomImp
import com.example.klaf.di.StorageSaveVersionRepositoryRoomImp
import com.example.klaf.domain.repositories.CardRepository
import com.example.klaf.domain.repositories.DeckRepository
import com.example.klaf.domain.repositories.StorageSaveVersionRepository
import com.example.klaf.domain.repositories.StorageTransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DeleteCardFromDeckUseCase @Inject constructor(
    @DeckRepositoryRoomImp
    private val deckRepository: DeckRepository,
    @CardRepositoryRoomImp
    private val cardRepository: CardRepository,
    @StorageSaveVersionRepositoryRoomImp
    private val localStorageSaveVersionRepository: StorageSaveVersionRepository,
    private val localStorageTransactionRepository: StorageTransactionRepository
) {

    suspend operator fun invoke(cardId: Int, deckId: Int) {
        withContext(Dispatchers.IO) {
            localStorageTransactionRepository.performWithTransaction {
                val originDeck = deckRepository.getDeckById(deckId = deckId)
                    ?: throw Exception("Fetching deck is failed")

                cardRepository.deleteCard(cardId = cardId)

                val actualCardQuantityInDeck =
                    cardRepository.fetchCardQuantityByDeckId(deckId = deckId)
                val updatedDeck = originDeck.copy(cardQuantity = actualCardQuantityInDeck)

                deckRepository.insertDeck(deck = updatedDeck)
                localStorageSaveVersionRepository.increaseVersion()
            }
        }
    }
}