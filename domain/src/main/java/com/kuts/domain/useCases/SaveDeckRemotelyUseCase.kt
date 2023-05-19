package com.kuts.domain.useCases

import com.kuts.domain.common.RemoteDeckRepositoryImp
import com.kuts.domain.entities.Deck
import com.kuts.domain.repositories.DeckRepository
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