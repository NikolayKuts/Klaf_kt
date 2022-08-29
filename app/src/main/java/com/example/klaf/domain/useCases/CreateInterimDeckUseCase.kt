package com.example.klaf.domain.useCases

import com.example.klaf.di.DeckRepositoryRoomImp
import com.example.klaf.domain.common.getCurrentDateAsLong
import com.example.klaf.domain.common.ifNull
import com.example.klaf.domain.entities.Deck
import com.example.klaf.domain.repositories.DeckRepository
import javax.inject.Inject

class CreateInterimDeckUseCase @Inject constructor(
    @DeckRepositoryRoomImp
    private val deckRepository: DeckRepository,
) {

    suspend operator fun invoke() {
        val interimDeck = deckRepository.getDeckById(deckId = Deck.INTERIM_DECK_ID)

        interimDeck.ifNull {
            deckRepository.insertDeck(
                deck = Deck(
                    name = Deck.INTERIM_DECK_NAME,
                    creationDate = getCurrentDateAsLong(),
                    repetitionIterationDates = listOf(),
                    scheduledIterationDates = listOf(),
                    scheduledDateInterval = 0,
                    repetitionQuantity = 0,
                    cardQuantity = 0,
                    lastFirstRepetitionDuration = 0,
                    lastSecondRepetitionDuration = 0,
                    lastRepetitionIterationDuration = 0,
                    isLastIterationSucceeded = false,
                    id = Deck.INTERIM_DECK_ID
                )
            )
        }
    }
}