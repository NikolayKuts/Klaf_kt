package com.kuts.domain.useCases

import com.kuts.domain.entities.Deck
import com.kuts.domain.repositories.IDeckRepository
import kotlinx.coroutines.flow.Flow

class FetchDeckSourceUseCase(
    private val deckRepository: IDeckRepository,
) {

    operator fun invoke(): Flow<List<Deck>> = deckRepository.fetchDeckSource()
}