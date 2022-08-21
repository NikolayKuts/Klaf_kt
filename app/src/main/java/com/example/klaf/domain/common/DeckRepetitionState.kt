package com.example.klaf.domain.common

import com.example.klaf.domain.entities.Card

data class DeckRepetitionState(
    val card: Card?,
    val side: CardSide,
    val repetitionOrder: CardRepetitionOrder,
)
