package com.example.klaf.presentation.theme

import androidx.compose.material.Colors
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.ui.graphics.Color

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
)

val DarkMainPalettes = MainColors(
    materialColors = darkColors(
//        primary = Color(0xFFC2665E),
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
)