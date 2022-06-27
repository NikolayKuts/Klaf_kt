package com.example.klaf.domain.useCases

import com.example.klaf.domain.entities.Card
import com.example.klaf.domain.repositories.CardRepository
import com.example.klaf.domain.repositories.DeckRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AddNewCardIntoDeckUseCase @Inject constructor(
    private val deckRepository: DeckRepository,
    private val cardRepository: CardRepository,
) {

    suspend operator fun invoke(card: Card) {
        withContext(Dispatchers.IO) {
            val originalDeck = deckRepository.getDeckById(deckId = card.deckId)
                ?: throw Exception("Fetching deck is failed")

            val updatedDeck = originalDeck.copy(cardQuantity = originalDeck.cardQuantity + 1)

            cardRepository.insertCard(card = card)
            deckRepository.insertDeck(deck = updatedDeck)
        }
    }
}