package com.kuts.domain.useCases

import com.kuts.domain.entities.Card
import com.kuts.domain.repositories.ICardRepository
import kotlinx.coroutines.flow.Flow

class FetchCardUseCase(
    private val cardRepository: ICardRepository,
) {

    operator fun invoke(cardId: Int): Flow<Card?> {
        return cardRepository.fetchObservableCardById(cardId = cardId)
    }
}