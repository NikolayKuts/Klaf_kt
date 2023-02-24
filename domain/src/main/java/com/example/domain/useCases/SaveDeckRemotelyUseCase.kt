package com.example.domain.useCases

import com.example.domain.common.RemoteDeckRepositoryImp
import com.example.domain.entities.Deck
import com.example.domain.repositories.DeckRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SaveDeckRemotelyUseCase @Inject constructor(
    @RemoteDeckRepositoryImp
    private val deckRepository: DeckRepository,
) {

    suspend operator fun invoke(deck: Deck) {
        withContext(Dispatchers.IO) { deckRepository.insertDeck(deck = deck) }
    }
}