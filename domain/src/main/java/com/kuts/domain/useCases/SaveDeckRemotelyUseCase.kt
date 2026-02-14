package com.kuts.domain.useCases

import com.kuts.domain.entities.Deck
import com.kuts.domain.repositories.IDeckRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SaveDeckRemotelyUseCase(
    private val deckRepository: IDeckRepository,
) {

    suspend operator fun invoke(deck: Deck) {
        withContext(Dispatchers.IO) { deckRepository.insertDeck(deck = deck) }
    }
}