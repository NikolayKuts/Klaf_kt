package com.example.klaf.presentation.cardTransferring.common

import com.example.domain.entities.Card

data class SelectableCardHolder(
    val card: Card,
    val isSelected: Boolean = false,
)
