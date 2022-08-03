package com.example.klaf.presentation.theme

import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

private const val MAIN_TEXT_SIZE_IN_PIXELS = 16

data class MainDimensions(
    val mainTextSize: TextUnit,
    val deckItemDeckName: TextUnit,
    val deckItemPointer: TextUnit,
    val dialogTextLineHeight: TextUnit,
)

val CommonDimension = MainDimensions(
    mainTextSize = MAIN_TEXT_SIZE_IN_PIXELS.sp,
    deckItemDeckName = 18.sp,
    deckItemPointer = 10.sp,
    dialogTextLineHeight = (MAIN_TEXT_SIZE_IN_PIXELS + 10).sp
)
