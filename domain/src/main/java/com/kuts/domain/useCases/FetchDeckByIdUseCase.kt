package com.kuts.domain.useCases

import com.kuts.domain.common.LocalDeckRepository
import com.kuts.domain.entities.Deck
import com.kuts.domain.repositories.IDeckRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FetchDeckByIdUseCase @Inject constructor(
    @LocalDeckRepository
    private val deckRepository: IDeckRepository,
) {

    operator fun invoke(deckId: Int): Flow<Deck?> {
        return deckRepository.fetchObservableDeckById(deckId = deckId)
    }
}