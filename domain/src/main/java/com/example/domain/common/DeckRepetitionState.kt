package com.example.domain.common

import com.example.domain.entities.Card

data class DeckRepetitionState(
    val card: Card?,
    val side: com.example.domain.common.CardSide,
    val repetitionOrder: com.example.domain.common.CardRepetitionOrder,
)
