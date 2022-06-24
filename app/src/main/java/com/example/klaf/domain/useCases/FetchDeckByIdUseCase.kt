package com.example.klaf.domain.useCases

import com.example.klaf.domain.entities.Deck
import com.example.klaf.domain.repositories.DeckRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FetchDeckByIdUseCase @Inject constructor(private val deckRepository: DeckRepository) {

    operator fun invoke(deckId: Int): Flow<Deck?> {
        return deckRepository.fetchObservableDeckById(deckId = deckId)
    }
}