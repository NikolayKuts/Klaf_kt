package com.kuts.domain.useCases

import com.kuts.domain.entities.Card
import com.kuts.domain.repositories.ICardRepository
import kotlinx.coroutines.flow.Flow

class FetchCardsUseCase(
    private val cardRepository: ICardRepository,
) {

    operator fun invoke(deckId: Int): Flow<List<Card>> {
        return cardRepository.fetchObservableCardsByDeckId(deckId = deckId)
    }
}