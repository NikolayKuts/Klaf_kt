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
    val dialogTextStyle: TextStyle,
    val accentedDialogText: SpanStyle,
    val cardPointer: SpanStyle,
    val cardAdditionPinterValue: SpanStyle,
    val frontSideOrderPointer: TextStyle,
    val backSideOrderPointer: TextStyle,
    val timerTextStyle: TextStyle,
    val cardWordTextStyle: TextStyle,
    val cardIpaPromptsTextStyle: TextStyle,
)

val Typography = Typography(
    body1 = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = CommonDimension.mainTextSize,
    ),
    caption = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = CommonDimension.captionTextSize,
    ),
    subtitle1 = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = CommonDimension.mainTextSize,
    ),
    button = TextStyle(
        color = Color.White,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        fontStyle = FontStyle.Italic,
        fontFamily = FontFamily.Monospace,
    ),
)

val LightMainTypographies = MainTopographies(
    materialTypographies = Typography,
    evenDeckItemName = LightEvenDeckItemNameTextStyle,
    oddDeckItemName = LightOddDeckItemNameTextStyle,
    scheduledDateRange = LightScheduledDateTextStyle,
    overdueScheduledDateRange = LightOverdueScheduledDateTextStyle,
    deckItemRepetitionQuantity = LightDeckItemRepetitionQuantitySpanStyle,
    deckItemCardQuantity = LightDeckItemCardQuantityTextStyle,
    deckItemPointer = LightDeckItemPointerTextStyle,
    dialogTextStyle = DialogTextStyle,
    accentedDialogText = AccentedDialogTextStyle,
    cardPointer = CardPointerTextStyle,
    cardAdditionPinterValue = CardPointerValueTextStyle,
    frontSideOrderPointer = LightFrondSideOrderPointer,
    backSideOrderPointer = LightBackSideOrderPointer,
    timerTextStyle = TimerTextStile,
    cardWordTextStyle = CardWordTextStyle,
    cardIpaPromptsTextStyle = IpaPromptsTextStyle,
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
    dialogTextStyle = DialogTextStyle,
    accentedDialogText = AccentedDialogTextStyle,
    cardPointer = CardPointerTextStyle,
    cardAdditionPinterValue = CardPointerValueTextStyle,
    frontSideOrderPointer = DarkFrondSideOrderPointer,
    backSideOrderPointer = DarkBackSideOrderPointer,
    timerTextStyle = TimerTextStile,
    cardWordTextStyle = CardWordTextStyle,
    cardIpaPromptsTextStyle = IpaPromptsTextStyle,
)