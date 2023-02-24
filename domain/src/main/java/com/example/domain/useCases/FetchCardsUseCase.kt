package com.example.domain.useCases

import com.example.domain.common.LocalCardRepositoryImp
import com.example.domain.entities.Card
import com.example.domain.repositories.CardRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FetchCardsUseCase @Inject constructor(
    @LocalCardRepositoryImp
    private val cardRepository: CardRepository,
) {

    operator fun invoke(deckId: Int): Flow<List<Card>> {
        return cardRepository.fetchObservableCardsByDeckId(deckId = deckId)
    }
}