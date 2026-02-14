package com.kuts.klaf.cardTransferring.common

import com.kuts.domain.entities.Card

data class SelectableCardHolder(
    val card: Card,
    val isSelected: Boolean = false,
)
