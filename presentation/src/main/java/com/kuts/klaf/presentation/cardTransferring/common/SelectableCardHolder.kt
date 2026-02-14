package com.kuts.klaf.presentation.cardTransferring.common

import com.kuts.domain.entities.Card

data class SelectableCardHolder(
    val card: Card,
    val isSelected: Boolean = false,
)
