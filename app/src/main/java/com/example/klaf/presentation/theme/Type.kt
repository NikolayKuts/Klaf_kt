package com.example.klaf.presentation.theme

import androidx.compose.material.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

data class MainTopographies(
    val materialTypographies: Typography,
    val evenDeckItemName: TextStyle,
    val oddDeckItemName: TextStyle,
    val scheduledDateRange: TextStyle,
    val overdueScheduledDateRange: TextStyle,
    val deckItemRepetitionQuantity: SpanStyle,
    val deckItemCardQuantity: SpanStyle,
    val deckItemPointer: SpanStyle,
)

private val Typography = Typography(
    body1 = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
    ),
    button = TextStyle(
        color = Color.White,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        fontStyle = FontStyle.Italic,
        fontFamily = FontFamily.Serif,
    ),
)



val LightMainTypographies = MainTopographies(
    materialTypographies = Typography,
    evenDeckItemName =  LightEvenDeckItemNameTextStyle,
    oddDeckItemName = LightOddDeckItemNameTextStyle,
    scheduledDateRange = LightScheduledDateTextStyle,
    overdueScheduledDateRange = LightOverdueScheduledDateTextStyle,
    deckItemRepetitionQuantity = LightDeckItemRepetitionQuantitySpanStyle,
    deckItemCardQuantity = LightDeckItemCardQuantityTextStyle,
    deckItemPointer = LightDeckItemPointerTextStyle,
)

val DarkMainTypographies = MainTopographies(
    materialTypographies = Typography,
    evenDeckItemName = DarkEvenDeckItemNameTextStyle,
    oddDeckItemName = DarkOddDeckItemNameTextStyle,
    scheduledDateRange = DarkScheduledDateTextStyle,
    overdueScheduledDateRange = DarkOverdueScheduledDateTextStyle,
    deckItemRepetitionQuantity = DarkDeckItemRepetitionQuantitySpanStyle,
    deckItemCardQuantity = DarkDeckItemCardQuantityTextStyle,
    deckItemPointer = DarkDeckItemPointerTextStyle,
)