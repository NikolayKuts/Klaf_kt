package com.example.klaf.presentation.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val MAIN_TEXT_SIZE_IN_PIXELS = 16

data class MainDimensions(
    val mainTextSize: TextUnit,
    val captionTextSize: TextUnit,
    val deckItemDeckName: TextUnit,
    val deckItemPointer: TextUnit,
    val dialogTextLineHeight: TextUnit,
    val dialogContentPadding: Dp,
    val cardAdditionPointerTextSize: TextUnit,
    val timerTextSize: TextUnit,
    val cardWordTextSize: TextUnit,
    val ipaPromptsTextSize: TextUnit,
)

val CommonDimension = MainDimensions(
    mainTextSize = MAIN_TEXT_SIZE_IN_PIXELS.sp,
    captionTextSize = 12.sp,
    deckItemDeckName = 18.sp,
    deckItemPointer = 10.sp,
    dialogTextLineHeight = (MAIN_TEXT_SIZE_IN_PIXELS + 10).sp,
    dialogContentPadding = 32.dp,
    cardAdditionPointerTextSize = 12.sp,
    timerTextSize = 18.sp,
    cardWordTextSize = 18.sp,
    ipaPromptsTextSize = 18.sp,
)
