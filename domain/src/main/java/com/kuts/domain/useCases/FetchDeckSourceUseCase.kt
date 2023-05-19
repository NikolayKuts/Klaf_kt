package com.kuts.domain.useCases

import com.kuts.domain.common.LocalDeckRepositoryImp
import com.kuts.domain.entities.Deck
import com.kuts.domain.repositories.DeckRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FetchDeckSourceUseCase @Inject constructor(
    @LocalDeckRepositoryImp
    private val deckRepository: DeckRepository,
) {

    operator fun invoke(): Flow<List<Deck>> = deckRepository.fetchDeckSource()
}