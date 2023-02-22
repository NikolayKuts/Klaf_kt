package com.example.domain.useCases

import com.example.domain.common.DeckRepositoryRoomImp
import com.example.domain.entities.Deck
import com.example.domain.repositories.DeckRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FetchDeckByIdUseCase @Inject constructor(
    @DeckRepositoryRoomImp
    private val deckRepository: DeckRepository,
) {

    operator fun invoke(deckId: Int): Flow<Deck?> {
        return deckRepository.fetchObservableDeckById(deckId = deckId)
    }
}