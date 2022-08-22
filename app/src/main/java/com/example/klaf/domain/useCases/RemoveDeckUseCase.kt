package com.example.klaf.domain.useCases

import com.example.klaf.di.DeckRepositoryRoomImp
import com.example.klaf.domain.repositories.DeckRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class RemoveDeckUseCase @Inject constructor(
    @DeckRepositoryRoomImp
    private val deckRepository: DeckRepository,
) {

    suspend operator fun invoke(deckId: Int) {
        withContext(Dispatchers.IO) { deckRepository.removeDeck(deckId = deckId) }
    }
}