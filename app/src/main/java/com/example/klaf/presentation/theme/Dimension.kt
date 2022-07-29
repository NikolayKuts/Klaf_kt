package com.example.klaf.presentation.theme

import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

data class MainDimensions(
    val deckItemDeckName: TextUnit,
    val deckItemPointer: TextUnit,
)

val CommonDimension = MainDimensions(
    deckItemDeckName = 18.sp,
    deckItemPointer = 10.sp
)
