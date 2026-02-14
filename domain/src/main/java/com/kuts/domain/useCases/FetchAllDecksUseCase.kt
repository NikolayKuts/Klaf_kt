package com.kuts.domain.useCases

import com.kuts.domain.common.LocalDeckRepository
import com.kuts.domain.entities.Deck
import com.kuts.domain.repositories.IDeckRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class FetchAllDecksUseCase @Inject constructor(
    @LocalDeckRepository
    private val deckRepository: IDeckRepository
) {

    suspend operator fun invoke(): List<Deck> = withContext(Dispatchers.IO) {
        deckRepository.fetchAllDecks()
    }
}