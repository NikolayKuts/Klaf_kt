package com.example.klaf.data.repositories

import com.example.klaf.domain.pojo.Card

interface RepetitionRepository {

    suspend fun getCardByDeckId(deckId: Int): List<Card>
}