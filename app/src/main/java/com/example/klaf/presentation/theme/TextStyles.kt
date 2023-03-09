package com.example.klaf.presentation.theme

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val MainFontFamily = FontFamily.Monospace

private val MainSpanStyle = SpanStyle(
    fontFamily = MainFontFamily,
    fontSize = CommonDimension.mainTextSize,
    fontStyle = FontStyle.Italic
)

val MainTextStyle = TextStyle(
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

val DarkEvenDeckItemNameTextStyle = MainTextStyle.copy(
    color = DarkMainPalettes.evenDeckItemName,
    fontSize = CommonDimension.deckItemDeckName,
    fontStyle = FontStyle.Italic,
)

val DarkOddDeckItemNameTextStyle = MainTextStyle.copy(
    color = DarkMainPalettes.oddDeckItemName,
    fontSize = CommonDimension.deckItemDeckName,
    fontStyle = FontStyle.Italic,
)

val LightEvenDeckItemNameTextStyle = MainTextStyle.copy(
    color = LightMainPalettes.evenDeckItemName,
    fontSize = CommonDimension.deckItemDeckName,
    fontStyle = FontStyle.Italic,
)

val LightOddDeckItemNameTextStyle = MainTextStyle.copy(
    color = LightMainPalettes.oddDeckItemName,
    fontSize = CommonDimension.deckItemDeckName,
    fontStyle = FontStyle.Italic,
)

val DarkDeckItemScheduledDateTextStyle = MainTextStyle.copy(
    color = DarkMainPalettes.scheduledDate,
    fontStyle = FontStyle.Italic,
)

val LightDeckItemScheduledDateTextStyle = MainTextStyle.copy(
    color = LightMainPalettes.scheduledDate,
    fontStyle = FontStyle.Italic,
)

val DarkDeckItemOverdueScheduledDateTextStyle = MainTextStyle.copy(
    color = DarkMainPalettes.overdueScheduledDate,
    fontStyle = FontStyle.Italic,
)

val LightDeckItemOverdueScheduledDateTextStyle = MainTextStyle.copy(
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

val DialogTextStyle = MainTextStyle.copy(
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

val LightFrondSideOrderPointer = MainTextStyle.copy(
    color = LightMainPalettes.deckRepetitionScreenColors.frontSideOrderPointer,
    fontWeight = FontWeight.Bold,
    fontStyle = FontStyle.Italic
)

val LightBackSideOrderPointer = MainTextStyle.copy(
    color = LightMainPalettes.deckRepetitionScreenColors.backSideOrderPointer,
    fontWeight = FontWeight.Bold,
    fontStyle = FontStyle.Italic
)

val DarkFrondSideOrderPointer = MainTextStyle.copy(
    color = DarkMainPalettes.deckRepetitionScreenColors.frontSideOrderPointer,
    fontWeight = FontWeight.Bold,
    fontStyle = FontStyle.Italic
)

val DarkBackSideOrderPointer = MainTextStyle.copy(
    color = DarkMainPalettes.deckRepetitionScreenColors.backSideOrderPointer,
    fontWeight = FontWeight.Bold,
    fontStyle = FontStyle.Italic
)

val TimerTextStile = TextStyle(
    fontSize = CommonDimension.timerTextSize,
    fontStyle = FontStyle.Italic
)

val LightFrontSideCardWordTextStyle = MainTextStyle.copy(
    fontSize = CommonDimension.cardWordTextSize,
    fontStyle = FontStyle.Italic,
    fontWeight = FontWeight.Bold,
    color = LightMainPalettes.deckRepetitionScreenColors.frontSideCardWord,
)

val DarkFrontSideCardWordTextStyle = MainTextStyle.copy(
    fontSize = CommonDimension.cardWordTextSize,
    fontStyle = FontStyle.Italic,
    fontWeight = FontWeight.Bold,
    color = DarkMainPalettes.deckRepetitionScreenColors.frontSideCardWord,
)

val LightBackSideCardWordTextStyle = MainTextStyle.copy(
    fontSize = CommonDimension.cardWordTextSize,
    fontStyle = FontStyle.Italic,
    fontWeight = FontWeight.Bold,
    color = LightMainPalettes.deckRepetitionScreenColors.backSideCardWord,
)

val DarkBackSideCardWordTextStyle = MainTextStyle.copy(
    fontSize = CommonDimension.cardWordTextSize,
    fontStyle = FontStyle.Italic,
    fontWeight = FontWeight.Bold,
    color = DarkMainPalettes.deckRepetitionScreenColors.backSideCardWord,
)


val IpaPromptsTextStyle = MainTextStyle.copy(
    fontSize = CommonDimension.ipaPromptsTextSize,
    fontStyle = FontStyle.Italic
)

private val ViewingCardTextStyle = MainTextStyle.copy(fontSize = CommonDimension.viewingCardContentTextSize)

val CommonViewingCardDeckNameTextStyle = MainTextStyle.copy(
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

private val HeaderTextStyle = MainTextStyle.copy(
    fontSize = CommonDimension.viewingCardDeckNameTextSize,
    fontStyle = FontStyle.Italic,
    fontWeight = FontWeight.Bold
)

private val PointerTitleTextStyle = MainTextStyle.copy(
    fontStyle = FontStyle.Italic,
)

private val QuantityPointer = MainTextStyle.copy(
    fontStyle = FontStyle.Italic
)

private val ChoosingContent = MainTextStyle.copy(
    fontStyle = FontStyle.Italic
)

val CannonCardTransferringScreenTextStyles = CardTransferringScreenTextStyles(
    header = HeaderTextStyle,
    pointerTitle = PointerTitleTextStyle,
    quantityPointerValue = QuantityPointer,
    choosingContent = ChoosingContent,
)

val LightForeignWordAutocompleteSpanStyle = MainSpanStyle.copy(
    color = LightMainPalettes.cardManagementViewColors.foreignWord,
    fontWeight = FontWeight.ExtraBold
)

val DarkForeignWordAutocompleteSpanStyle = MainSpanStyle.copy(
    color = DarkMainPalettes.cardManagementViewColors.foreignWord,
    fontWeight = FontWeight.ExtraBold
)

val LightIpaValueTextStyle = MainTextStyle.copy(color = LightMainPalettes.materialColors.onPrimary)

val DarkIpaValueTextStyle = MainTextStyle.copy(color = DarkMainPalettes.materialColors.onPrimary)

val commonDeckNavigationDialogTitle = MainTextStyle.copy(
    fontSize = 30.sp,
    fontStyle = FontStyle.Italic
)