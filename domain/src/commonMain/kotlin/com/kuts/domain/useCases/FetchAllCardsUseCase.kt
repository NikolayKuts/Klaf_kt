package com.kuts.domain.useCases

import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.entities.Card
import com.kuts.domain.repositories.ICardRepository
import kotlinx.coroutines.withContext

class FetchAllCardsUseCase(
    private val cardRepository: ICardRepository,
    private val coroutineContextProvider: ICoroutineContextProvider,
) {

    suspend operator fun invoke(): List<Card> = withContext(context = coroutineContextProvider.io) {
        cardRepository.fetchAllCards()
    }
}

