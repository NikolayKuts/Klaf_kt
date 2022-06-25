package com.example.klaf.domain.useCases

import com.example.klaf.domain.entities.Card
import com.example.klaf.domain.repositories.CardRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FetchCardsUseCase @Inject constructor(private val cardRepository: CardRepository) {

    operator fun invoke(deckId: Int): Flow<List<Card>> {
        return  cardRepository.getCardsByDeckId(deckId = deckId)
    }
}