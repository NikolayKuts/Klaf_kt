package com.example.klaf.data.repositories

import com.example.klaf.domain.pojo.Card

interface CardEditingRepository {

    suspend fun insertCard(card: Card)

    suspend fun getCardById(cardId: Int): Card
}