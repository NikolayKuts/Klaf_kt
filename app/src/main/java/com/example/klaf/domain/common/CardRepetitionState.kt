package com.example.klaf.domain.common

import com.example.klaf.domain.entities.Card

data class CardRepetitionState(
    val card: Card,
    val side: CardSide,
    val repetitionOrder: CardRepetitionOrder,
)
