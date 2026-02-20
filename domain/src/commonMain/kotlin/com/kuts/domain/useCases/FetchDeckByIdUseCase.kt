package com.kuts.domain.useCases

import com.kuts.domain.entities.Deck
import com.kuts.domain.repositories.IDeckRepository
import kotlinx.coroutines.flow.Flow

class FetchDeckByIdUseCase(
    private val deckRepository: IDeckRepository,
) {

    operator fun invoke(deckId: Int): Flow<Deck?> {
        return deckRepository.fetchObservableDeckById(deckId = deckId)
    }
}