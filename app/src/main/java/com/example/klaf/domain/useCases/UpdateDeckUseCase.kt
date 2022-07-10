package com.example.klaf.domain.useCases

import com.example.klaf.domain.entities.Deck
import com.example.klaf.domain.repositories.DeckRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UpdateDeckUseCase @Inject constructor(private val deckRepository: DeckRepository) {

    suspend operator fun invoke(updatedDeck: Deck) {
        withContext(Dispatchers.IO) { deckRepository.insertDeck(deck = updatedDeck) }
    }
}