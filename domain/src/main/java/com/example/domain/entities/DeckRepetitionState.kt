package com.example.domain.entities

import com.example.domain.common.CardRepetitionOrder
import com.example.domain.common.CardSide

data class DeckRepetitionState(
    val card: Card?,
    val side: CardSide,
    val repetitionOrder: CardRepetitionOrder,
)