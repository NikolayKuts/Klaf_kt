package com.example.klaf.presentation.theme

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

private val MainFontFamily = FontFamily.Monospace

private val MainSpanStyle = SpanStyle(
    fontFamily = MainFontFamily,
    fontSize = CommonDimension.mainTextSize,
    fontStyle = FontStyle.Italic
)

val Body1 = TextStyle(
    fontFamily = MainFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = CommonDimension.mainTextSize,
)

val Caption = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Normal,
    fontSize = CommonDimension.captionTextSize,
)

val Subtitle1 = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Normal,
    fontSize = CommonDimension.mainTextSize,
)

val Button = TextStyle(
    fontSize = CommonDimension.mainButtonTextSize,
    fontWeight = FontWeight.Bold,
    fontStyle = FontStyle.Italic,
    fontFamily = FontFamily.Monospace,
)

val DarkEvenDeckItemNameTextStyle = Body1.copy(
    color = DarkMainPalettes.evenDeckItemName,
    fontSize = CommonDimension.deckItemDeckName,
    fontStyle = FontStyle.Italic,
)

val DarkOddDeckItemNameTextStyle = Body1.copy(
    color = DarkMainPalettes.oddDeckItemName,
    fontSize = CommonDimension.deckItemDeckName,
    fontStyle = FontStyle.Italic,
)

val LightEvenDeckItemNameTextStyle = Body1.copy(
    color = LightMainPalettes.evenDeckItemName,
    fontSize = CommonDimension.deckItemDeckName,
    fontStyle = FontStyle.Italic,
)

val LightOddDeckItemNameTextStyle = Body1.copy(
    color = LightMainPalettes.oddDeckItemName,
    fontSize = CommonDimension.deckItemDeckName,
    fontStyle = FontStyle.Italic,
)

val DarkDeckItemScheduledDateTextStyle = Body1.copy(
    color = DarkMainPalettes.scheduledDate,
    fontStyle = FontStyle.Italic,
)

val LightDeckItemScheduledDateTextStyle = Body1.copy(
    color = LightMainPalettes.scheduledDate,
    fontStyle = FontStyle.Italic,
)

val DarkDeckItemOverdueScheduledDateTextStyle = Body1.copy(
    color = DarkMainPalettes.overdueScheduledDate,
    fontStyle = FontStyle.Italic,
)

val LightDeckItemOverdueScheduledDateTextStyle = Body1.copy(
    color = LightMainPalettes.overdueScheduledDate,
    fontStyle = FontStyle.Italic,
)

val DarkDeckItemRepetitionQuantitySpanStyle = MainSpanStyle.copy(
    color = DarkMainPalettes.deckItemRepetitionQuantity
)

val LightDeckItemRepetitionQuantitySpanStyle = MainSpanStyle.copy(
    color = LightMainPalettes.deckItemRepetitionQuantity
)

val DarkDeckItemCardQuantityTextStyle = MainSpanStyle.copy(
    color = DarkMainPalettes.deckItemCardQuantity,
)

val LightDeckItemCardQuantityTextStyle = MainSpanStyle.copy(
    color = LightMainPalettes.deckItemCardQuantity,
)

val DarkDeckItemPointerTextStyle = MainSpanStyle.copy(
    color = DarkMainPalettes.deckItemPointer,
    fontSize = CommonDimension.deckItemPointer
)

val LightDeckItemPointerTextStyle = MainSpanStyle.copy(
    color = LightMainPalettes.deckItemPointer,
    fontSize = CommonDimension.deckItemPointer
)

val DialogTextStyle = Body1.copy(
    lineHeight = CommonDimension.dialogTextLineHeight
)

val AccentedDialogTextStyle = MainSpanStyle.copy(
    fontStyle = FontStyle.Italic,
    fontWeight = FontWeight.Bold
)

val CardPointerTextStyle = MainSpanStyle.copy(
    fontStyle = FontStyle.Italic,
    fontSize = CommonDimension.cardAdditionPointerTextSize
)

val CardPointerValueTextStyle = MainSpanStyle.copy(
    fontWeight = FontWeight.Bold,
    fontStyle = FontStyle.Italic,
)

val LightFrondSideOrderPointer = Body1.copy(
    color = LightMainPalettes.frontSideOrderPointer
)

val LightBackSideOrderPointer = Body1.copy(
    color = LightMainPalettes.backSideOrderPointer
)

val DarkFrondSideOrderPointer = Body1.copy(
    color = DarkMainPalettes.frontSideOrderPointer
)

val DarkBackSideOrderPointer = Body1.copy(
    color = DarkMainPalettes.backSideOrderPointer
)

val TimerTextStile = TextStyle(
    fontSize = CommonDimension.timerTextSize,
    fontStyle = FontStyle.Italic
)

val CardWordTextStyle = Body1.copy(
    fontSize = CommonDimension.cardWordTextSize,
    fontStyle = FontStyle.Italic
)

val IpaPromptsTextStyle = Body1.copy(
    fontSize = CommonDimension.ipaPromptsTextSize,
    fontStyle = FontStyle.Italic
)