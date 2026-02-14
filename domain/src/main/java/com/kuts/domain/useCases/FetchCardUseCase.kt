package com.kuts.domain.useCases

import com.kuts.domain.common.LocalCardRepository
import com.kuts.domain.entities.Card
import com.kuts.domain.repositories.ICardRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FetchCardUseCase @Inject constructor(
    @LocalCardRepository
    private val cardRepository: ICardRepository,
) {

    operator fun invoke(cardId: Int): Flow<Card?> {
        return cardRepository.fetchObservableCardById(cardId = cardId)
    }
}