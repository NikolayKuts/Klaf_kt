package com.example.klaf.domain.useCases

import com.example.klaf.di.DeckRepositoryRoomImp
import com.example.klaf.domain.entities.Deck
import com.example.klaf.domain.repositories.DeckRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class RenameDeckUseCase @Inject constructor(
    @DeckRepositoryRoomImp
    private val deckRepository: DeckRepository,
) {

    suspend operator fun invoke(oldDeck: Deck, name: String) {
        withContext(Dispatchers.IO) {
            deckRepository.insertDeck(deck = oldDeck.copy(name = name))
        }
    }
}