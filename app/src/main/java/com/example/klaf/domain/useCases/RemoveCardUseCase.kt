package com.example.klaf.domain.useCases

import com.example.klaf.domain.repositories.CardRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class RemoveCardUseCase @Inject constructor(private val cardRepository: CardRepository) {

    suspend operator fun invoke(cardId: Int) {
        withContext(Dispatchers.IO) {
            cardRepository.deleteCard(cardId = cardId)
        }
    }
}