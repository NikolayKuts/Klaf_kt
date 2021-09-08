package com.example.klaf.data.repositories

import com.example.klaf.domain.pojo.Card

interface CardAdditionRepository {

    suspend fun insertCard(card: Card)
}