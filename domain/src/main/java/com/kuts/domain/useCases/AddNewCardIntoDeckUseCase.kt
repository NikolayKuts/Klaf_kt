package com.kuts.domain.useCases

import com.kuts.domain.common.LocalCardRepository
import com.kuts.domain.common.LocalDeckRepository
import com.kuts.domain.common.LocalStorageSaveVersionRepository
import com.kuts.domain.entities.Card
import com.kuts.domain.repositories.ICardRepository
import com.kuts.domain.repositories.IDeckRepository
import com.kuts.domain.repositories.IStorageSaveVersionRepository
import com.kuts.domain.repositories.IStorageTransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AddNewCardIntoDeckUseCase @Inject constructor(
    @LocalDeckRepository
    private val deckRepository: IDeckRepository,
    @LocalCardRepository
    private val cardRepository: ICardRepository,
    @LocalStorageSaveVersionRepository
    private val localStorageSaveVersionRepository: IStorageSaveVersionRepository,
    private val localStorageTransactionRepository: IStorageTransactionRepository,
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

