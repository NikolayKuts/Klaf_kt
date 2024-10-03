package com.kuts.domain.useCases

import com.kuts.domain.common.LocalCardRepositoryImp
import com.kuts.domain.entities.Deck
import com.kuts.domain.repositories.CardRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CheckIfCardExistsUseCase @Inject constructor(
    @LocalCardRepositoryImp
    private val cardRepository: CardRepository,
) {

    suspend operator fun invoke(foreignWord: String): List<Deck> = withContext(Dispatchers.IO) {
        cardRepository.checkIfCardExists(foreignWord = foreignWord)
    }
}