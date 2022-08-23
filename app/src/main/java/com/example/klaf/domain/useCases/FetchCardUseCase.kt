package com.example.klaf.domain.useCases

import com.example.klaf.di.CardRepositoryRoomImp
import com.example.klaf.domain.entities.Card
import com.example.klaf.domain.repositories.CardRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FetchCardUseCase @Inject constructor(
    @CardRepositoryRoomImp
    private val cardRepository: CardRepository,
) {

    operator fun invoke(cardId: Int): Flow<Card?> {
        return cardRepository.getObservableCardById(cardId = cardId)
    }
}