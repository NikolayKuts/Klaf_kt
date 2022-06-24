package com.example.klaf.domain.useCases

import com.example.klaf.domain.entities.Card
import com.example.klaf.domain.repositories.CardRepository
import javax.inject.Inject

class CreateCardUseCase @Inject constructor(private val cardRepository: CardRepository) {

    suspend operator fun invoke(card: Card) {
        cardRepository.insertCard(card = card)
    }
}