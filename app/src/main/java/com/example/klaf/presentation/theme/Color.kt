package com.example.klaf.presentation.theme

import androidx.compose.material.Colors
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.ui.graphics.Color

private val DarkPrimaryColor = Color(0xFF5DA3AC)

data class MainColors(
    val materialColors: Colors,
    val lightDeckItemBackground: Color,
    val darkDeckItemBackground: Color,
    val evenDeckItemName: Color,
    val oddDeckItemName: Color,
    val scheduledDate: Color,
    val overdueScheduledDate: Color,
    val deckItemRepetitionQuantity: Color,
    val deckItemCardQuantity: Color,
    val deckItemPointer: Color,
    val positiveDialogButton: Color,
    val negativeDialogButton: Color,
    val neutralDialogButton: Color,
    val checkedLetterCell: Color,
    val uncheckedLetterCell: Color,
    val cardAdditionTextField: Color,
)

val LightMainPalettes = MainColors(
    materialColors = lightColors(
        primary = Color(0xFF8BC34A)
    ),
    lightDeckItemBackground = Color(0xFF8BC34A),
    darkDeckItemBackground = Color(0xFF8BC34A),
    evenDeckItemName = Color(0xFF8BC34A),
    oddDeckItemName = Color(0xFF8BC34A),
    scheduledDate = Color(0xFF8BC34A),
    overdueScheduledDate = Color(0xFF8BC34A),
    deckItemRepetitionQuantity = Color(0xFF8BC34A),
    deckItemCardQuantity = Color(0xFF8BC34A),
    deckItemPointer = Color(0xFF8BC34A),
    positiveDialogButton = Color(0xFF8BC34A),
    negativeDialogButton = Color(0xFF8BC34A),
    neutralDialogButton = Color(0xFF8BC34A),
    checkedLetterCell = Color(0xFF8BC34A),
    uncheckedLetterCell = Color(0xFF8BC34A),
    cardAdditionTextField = Color(0xFF8BC34A),
)

val DarkMainPalettes = MainColors(
    materialColors = darkColors(
        primary = DarkPrimaryColor,
//        onPrimary = Color(0xFFFFFFFF),
//        primaryVariant = Color(0xFF89F53C),
//        secondary = Color(0xFF4CAF50),
//        onSecondary = Color(0xFFEFF53C)
    ),

    lightDeckItemBackground = Color(0xFF525252),
    darkDeckItemBackground = Color(0xFF3D3D3D),
    evenDeckItemName = Color(0xFF8BC34A),
    oddDeckItemName = Color(0xFFC9C9C9),
    scheduledDate = Color(0xFFD69E4A),
    overdueScheduledDate = Color(0xFFDA8282),
    deckItemRepetitionQuantity = Color(0xFF56C2CF),
    deckItemCardQuantity = Color(0xFFD5C85B),
    deckItemPointer = Color(0xFF969696),
    positiveDialogButton = Color(0xFF87B156),
    negativeDialogButton = Color(0xFFD17670),
    neutralDialogButton = DarkPrimaryColor,
    checkedLetterCell = Color(0xFF94B172),
    uncheckedLetterCell = Color(0xFF63665F),
    cardAdditionTextField = Color(0x000000),
)