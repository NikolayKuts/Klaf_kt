package com.example.domain.useCases

import com.example.domain.common.LocalDeckRepositoryImp
import com.example.domain.entities.Deck
import com.example.domain.repositories.DeckRepository
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