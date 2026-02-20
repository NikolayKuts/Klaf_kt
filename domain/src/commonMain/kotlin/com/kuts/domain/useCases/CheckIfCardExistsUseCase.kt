package com.kuts.domain.useCases

import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.entities.Deck
import com.kuts.domain.repositories.ICardRepository
import kotlinx.coroutines.withContext

class CheckIfCardExistsUseCase(
    private val cardRepository: ICardRepository,
    private val coroutineContextProvider: ICoroutineContextProvider,
) {

    suspend operator fun invoke(foreignWord: String): List<Deck> {
        return withContext(context = coroutineContextProvider.io) {
            cardRepository.checkIfCardExists(foreignWord = foreignWord)
        }
    }
}
