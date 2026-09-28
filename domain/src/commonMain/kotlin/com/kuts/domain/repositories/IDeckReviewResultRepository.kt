package com.kuts.domain.repositories

import com.kuts.domain.entities.Deck
import com.kuts.domain.entities.DeckRepetitionInfo

interface IDeckReviewResultRepository {

    suspend fun save(updatedDeck: Deck, reviewInfo: DeckRepetitionInfo)
}
