package com.kuts.domain.useCases

import com.kuts.domain.entities.Card
import com.kuts.domain.repositories.ICardRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SaveCardRemotelyUseCase(
    private val cardRepository: ICardRepository,
) {

    suspend operator fun invoke(card: Card) {
        withContext(Dispatchers.IO) { cardRepository.insertCard(card = card) }
    }
}