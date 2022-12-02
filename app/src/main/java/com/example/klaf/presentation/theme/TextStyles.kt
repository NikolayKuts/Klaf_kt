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
    color = LightMainPalettes.frontSideOrderPointer,
    fontWeight = FontWeight.Bold,
    fontStyle = FontStyle.Italic
)

val LightBackSideOrderPointer = Body1.copy(
    color = LightMainPalettes.backSideOrderPointer,
    fontWeight = FontWeight.Bold,
    fontStyle = FontStyle.Italic
)

val DarkFrondSideOrderPointer = Body1.copy(
    color = DarkMainPalettes.frontSideOrderPointer,
    fontWeight = FontWeight.Bold,
    fontStyle = FontStyle.Italic
)

val DarkBackSideOrderPointer = Body1.copy(
    color = DarkMainPalettes.backSideOrderPointer,
    fontWeight = FontWeight.Bold,
    fontStyle = FontStyle.Italic
)

val TimerTextStile = TextStyle(
    fontSize = CommonDimension.timerTextSize,
    fontStyle = FontStyle.Italic
)

val LightFrontSideCardWordTextStyle = Body1.copy(
    fontSize = CommonDimension.cardWordTextSize,
    fontStyle = FontStyle.Italic,
    fontWeight = FontWeight.Bold,
    color = LightMainPalettes.frontSideCardWord,
)

val DarkFrontSideCardWordTextStyle = Body1.copy(
    fontSize = CommonDimension.cardWordTextSize,
    fontStyle = FontStyle.Italic,
    fontWeight = FontWeight.Bold,
    color = DarkMainPalettes.frontSideCardWord,
)

val LightBackSideCardWordTextStyle = Body1.copy(
    fontSize = CommonDimension.cardWordTextSize,
    fontStyle = FontStyle.Italic,
    fontWeight = FontWeight.Bold,
    color = LightMainPalettes.backSideCardWord,
)

val DarkBackSideCardWordTextStyle = Body1.copy(
    fontSize = CommonDimension.cardWordTextSize,
    fontStyle = FontStyle.Italic,
    fontWeight = FontWeight.Bold,
    color = DarkMainPalettes.backSideCardWord,
)


val IpaPromptsTextStyle = Body1.copy(
    fontSize = CommonDimension.ipaPromptsTextSize,
    fontStyle = FontStyle.Italic
)

private val ViewingCardTextStyle = Body1.copy(fontSize = CommonDimension.viewingCardContentTextSize)

val CommonViewingCardDeckNameTextStyle = Body1.copy(
    fontSize = CommonDimension.viewingCardDeckNameTextSize,
    fontStyle = FontStyle.Italic,
    fontWeight = FontWeight.Bold
)

val LightViewingCardNativeWord = ViewingCardTextStyle

val LightViewingCardForeignWord = ViewingCardTextStyle.copy(
    color = LightMainPalettes.viewingCardForeignWord
)

val LightViewingCardIpa = ViewingCardTextStyle.copy(
    color = LightMainPalettes.viewingCardIpa
)

val LightViewingCardOrdinal = ViewingCardTextStyle.copy(
    color = LightMainPalettes.viewingCardOrdinal,
    fontStyle = FontStyle.Italic
)

val DarkViewingCardNativeWord = ViewingCardTextStyle

val DarkViewingCardForeignWord = ViewingCardTextStyle.copy(
    color = DarkMainPalettes.viewingCardForeignWord
)

val DarkViewingCardIpa = ViewingCardTextStyle.copy(
    color = DarkMainPalettes.viewingCardIpa
)

val DarkViewingCardOrdinal = ViewingCardTextStyle.copy(
    color = DarkMainPalettes.viewingCardOrdinal,
    fontStyle = FontStyle.Italic
)

private val HeaderTextStyle = Body1.copy(
    fontSize = CommonDimension.viewingCardDeckNameTextSize,
    fontStyle = FontStyle.Italic,
    fontWeight = FontWeight.Bold
)

private val PointerTitleTextStyle = Body1.copy(
    fontStyle = FontStyle.Italic,
)

private val QuantityPointer = Body1.copy(
    fontStyle = FontStyle.Italic
)

private val ChoosingContent = Body1.copy(
    fontStyle = FontStyle.Italic
)

val CannonCardTransferringScreenTextStyles = CardTransferringScreenTextStyles(
    header = HeaderTextStyle,
    pointerTitle = PointerTitleTextStyle,
    quantityPointerValue = QuantityPointer,
    choosingContent = ChoosingContent,
)