package com.example.klaf.domain.useCases

import com.example.klaf.domain.repositories.CardRepository
import com.example.klaf.domain.repositories.DeckRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DeleteCardFromDeckUseCase @Inject constructor(
    private val deckRepository: DeckRepository,
    private val cardRepository: CardRepository,
) {

    suspend operator fun invoke(cardId: Int, deckId: Int) {
        withContext(Dispatchers.IO) {
            val originDeck = deckRepository.getDeckById(deckId = deckId)
                ?: throw Exception("Fetching deck is failed")

            cardRepository.deleteCard(cardId = cardId)

            val actualCardQuantityInDeck = cardRepository.getCardQuantityByDeckId(deckId = deckId)
            val updatedDeck = originDeck.copy(cardQuantity = actualCardQuantityInDeck)

            deckRepository.insertDeck(deck = updatedDeck)
        }
    }
}