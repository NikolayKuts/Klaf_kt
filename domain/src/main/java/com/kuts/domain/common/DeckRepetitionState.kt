package com.kuts.domain.common

import com.kuts.domain.entities.Card

data class DeckRepetitionState(
    val card: Card?,
    val side: CardSide,
    val repetitionOrder: CardRepetitionOrder,
)
