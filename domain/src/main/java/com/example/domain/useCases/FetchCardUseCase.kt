package com.example.domain.useCases

import com.example.domain.common.CardRepositoryRoomImp
import com.example.domain.entities.Card
import com.example.domain.repositories.CardRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FetchCardUseCase @Inject constructor(
    @CardRepositoryRoomImp
    private val cardRepository: CardRepository,
) {

    operator fun invoke(cardId: Int): Flow<Card?> {
        return cardRepository.fetchObservableCardById(cardId = cardId)
    }
}