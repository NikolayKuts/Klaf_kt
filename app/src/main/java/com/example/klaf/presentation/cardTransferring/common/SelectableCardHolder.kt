package com.example.klaf.presentation.cardTransferring.common

import com.example.klaf.domain.entities.Card

data class SelectableCardHolder(
    val card: Card,
    val isSelected: Boolean = false,
)
