package com.example.klaf.domain.useCases

import com.example.klaf.di.DeckRepositoryFirestoreImp
import com.example.klaf.domain.entities.Deck
import com.example.klaf.domain.repositories.DeckRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SaveDeckRemotelyUseCase @Inject constructor(
    @DeckRepositoryFirestoreImp
    private val deckRepository: DeckRepository,
) {

    suspend operator fun invoke(deck: Deck) {
        withContext(Dispatchers.IO) { deckRepository.insertDeck(deck = deck) }
    }
}