package com.kuts.domain.useCases

import com.kuts.domain.common.LocalCardRepositoryImp
import com.kuts.domain.entities.Card
import com.kuts.domain.repositories.CardRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FetchCardUseCase @Inject constructor(
    @LocalCardRepositoryImp
    private val cardRepository: CardRepository,
) {

    operator fun invoke(cardId: Int): Flow<Card?> {
        return cardRepository.fetchObservableCardById(cardId = cardId)
    }
}