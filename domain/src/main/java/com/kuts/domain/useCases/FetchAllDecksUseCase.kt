package com.kuts.domain.useCases

import com.kuts.domain.common.LocalDeckRepositoryImp
import com.kuts.domain.entities.Deck
import com.kuts.domain.repositories.DeckRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class FetchAllDecksUseCase @Inject constructor(
    @LocalDeckRepositoryImp
    private val deckRepository: DeckRepository
) {

    suspend operator fun invoke(): List<Deck> = withContext(Dispatchers.IO) {
        deckRepository.fetchAllDecks()
    }
}