package com.kuts.domain.useCases

import com.kuts.domain.common.LocalDeckRepository
import com.kuts.domain.entities.Deck
import com.kuts.domain.repositories.IDeckRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FetchDeckSourceUseCase @Inject constructor(
    @LocalDeckRepository
    private val deckRepository: IDeckRepository,
) {

    operator fun invoke(): Flow<List<Deck>> = deckRepository.fetchDeckSource()
}