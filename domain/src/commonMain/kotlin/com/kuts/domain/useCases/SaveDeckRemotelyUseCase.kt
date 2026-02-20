package com.kuts.domain.useCases

import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.entities.Deck
import com.kuts.domain.repositories.IDeckRepository
import kotlinx.coroutines.withContext

class SaveDeckRemotelyUseCase(
    private val deckRepository: IDeckRepository,
    private val coroutineContextProvider: ICoroutineContextProvider,
) {

    suspend operator fun invoke(deck: Deck) {
        withContext(context = coroutineContextProvider.io) {
            deckRepository.insertDeck(deck = deck)
        }
    }
}
