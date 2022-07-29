package com.example.klaf.domain.useCases

import com.example.klaf.domain.entities.Deck
import com.example.klaf.domain.repositories.DeckRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FetchDeckSourceUseCase @Inject constructor(private val deckRepository: DeckRepository) {

    operator fun invoke(): Flow<List<Deck>> = deckRepository.fetchDeckSource()
}