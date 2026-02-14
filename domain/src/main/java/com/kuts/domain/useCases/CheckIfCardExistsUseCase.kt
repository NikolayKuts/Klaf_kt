package com.kuts.domain.useCases

import com.kuts.domain.entities.Deck
import com.kuts.domain.repositories.ICardRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CheckIfCardExistsUseCase(
    private val cardRepository: ICardRepository,
) {

    suspend operator fun invoke(foreignWord: String): List<Deck> = withContext(Dispatchers.IO) {
        cardRepository.checkIfCardExists(foreignWord = foreignWord)
    }
}