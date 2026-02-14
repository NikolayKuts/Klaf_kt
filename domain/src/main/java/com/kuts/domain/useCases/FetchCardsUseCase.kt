package com.kuts.domain.useCases

import com.kuts.domain.common.LocalCardRepository
import com.kuts.domain.entities.Card
import com.kuts.domain.repositories.ICardRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FetchCardsUseCase @Inject constructor(
    @LocalCardRepository
    private val cardRepository: ICardRepository,
) {

    operator fun invoke(deckId: Int): Flow<List<Card>> {
        return cardRepository.fetchObservableCardsByDeckId(deckId = deckId)
    }
}