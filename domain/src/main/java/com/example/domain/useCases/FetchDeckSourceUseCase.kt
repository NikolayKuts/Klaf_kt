package com.example.domain.useCases

import com.example.domain.common.LocalDeckRepositoryImp
import com.example.domain.entities.Deck
import com.example.domain.repositories.DeckRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FetchDeckSourceUseCase @Inject constructor(
    @LocalDeckRepositoryImp
    private val deckRepository: DeckRepository,
) {

    operator fun invoke(): Flow<List<Deck>> = deckRepository.fetchDeckSource()
}