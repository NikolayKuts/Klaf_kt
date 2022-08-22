package com.example.klaf.domain.useCases

import com.example.klaf.di.DeckRepositoryRoomImp
import com.example.klaf.domain.entities.Card
import com.example.klaf.domain.repositories.CardRepository
import com.example.klaf.domain.repositories.DeckRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AddNewCardIntoDeckUseCase @Inject constructor(
    @DeckRepositoryRoomImp
    private val deckRepository: DeckRepository,
    private val cardRepository: CardRepository,
) {

    suspend operator fun invoke(card: Card) {
        withContext(Dispatchers.IO) {
            val originalDeck = deckRepository.getDeckById(deckId = card.deckId)
                ?: throw Exception("Fetching deck is failed")

            cardRepository.insertCard(card = card)
            val actualCardQuantity = cardRepository.getCardQuantityByDeckId(deckId = card.deckId)
            val updatedDeck = originalDeck.copy(cardQuantity = actualCardQuantity)

            deckRepository.insertDeck(deck = updatedDeck)
        }
    }
}

