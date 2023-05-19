package com.kuts.domain.useCases

import com.kuts.domain.common.LocalCardRepositoryImp
import com.kuts.domain.entities.Card
import com.kuts.domain.repositories.CardRepository
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