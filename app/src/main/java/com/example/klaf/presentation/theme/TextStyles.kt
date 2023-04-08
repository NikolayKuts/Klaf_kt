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
    color = DarkMainPalettes.deckListScreen.evenDeckItemName,
    fontSize = CommonDimension.deckItemDeckName,
    fontStyle = FontStyle.Italic,
)

val DarkOddDeckItemNameTextStyle = MainTextStyle.copy(
    color = DarkMainPalettes.deckListScreen.oddDeckItemName,
    fontSize = CommonDimension.deckItemDeckName,
    fontStyle = FontStyle.Italic,
)

val LightEvenDeckItemNameTextStyle = MainTextStyle.copy(
    color = LightMainPalettes.deckListScreen.evenDeckItemName,
    fontSize = CommonDimension.deckItemDeckName,
    fontStyle = FontStyle.Italic,
)

val LightOddDeckItemNameTextStyle = MainTextStyle.copy(
    color = LightMainPalettes.deckListScreen.oddDeckItemName,
    fontSize = CommonDimension.deckItemDeckName,
    fontStyle = FontStyle.Italic,
)

val DarkDeckItemScheduledDateTextStyle = MainTextStyle.copy(
    color = DarkMainPalettes.deckListScreen.scheduledDate,
    fontStyle = FontStyle.Italic,
)

val LightDeckItemScheduledDateTextStyle = MainTextStyle.copy(
    color = LightMainPalettes.deckListScreen.scheduledDate,
    fontStyle = FontStyle.Italic,
)

val DarkDeckItemOverdueScheduledDateTextStyle = MainTextStyle.copy(
    color = DarkMainPalettes.deckListScreen.overdueScheduledDate,
    fontStyle = FontStyle.Italic,
)

val LightDeckItemOverdueScheduledDateTextStyle = MainTextStyle.copy(
    color = LightMainPalettes.deckListScreen.overdueScheduledDate,
    fontStyle = FontStyle.Italic,
)

val DarkDeckItemRepetitionQuantitySpanStyle = MainSpanStyle.copy(
    color = DarkMainPalettes.deckListScreen.deckItemRepetitionQuantity
)

val LightDeckItemRepetitionQuantitySpanStyle = MainSpanStyle.copy(
    color = LightMainPalettes.deckListScreen.deckItemRepetitionQuantity
)

val DarkDeckItemCardQuantityTextStyle = MainSpanStyle.copy(
    color = DarkMainPalettes.deckListScreen.deckItemCardQuantity,
)

val LightDeckItemCardQuantityTextStyle = MainSpanStyle.copy(
    color = LightMainPalettes.deckListScreen.deckItemCardQuantity,
)

val DarkDeckItemPointerTextStyle = MainSpanStyle.copy(
    color = DarkMainPalettes.deckListScreen.deckItemPointer,
    fontSize = CommonDimension.deckItemPointer
)

val LightDeckItemPointerTextStyle = MainSpanStyle.copy(
    color = LightMainPalettes.deckListScreen.deckItemPointer,
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
    color = LightMainPalettes.deckRepetitionScreen.frontSideOrderPointer,
    fontWeight = FontWeight.Bold,
    fontStyle = FontStyle.Italic
)

val LightBackSideOrderPointer = MainTextStyle.copy(
    color = LightMainPalettes.deckRepetitionScreen.backSideOrderPointer,
    fontWeight = FontWeight.Bold,
    fontStyle = FontStyle.Italic
)

val DarkFrondSideOrderPointer = MainTextStyle.copy(
    color = DarkMainPalettes.deckRepetitionScreen.frontSideOrderPointer,
    fontWeight = FontWeight.Bold,
    fontStyle = FontStyle.Italic
)

val DarkBackSideOrderPointer = MainTextStyle.copy(
    color = DarkMainPalettes.deckRepetitionScreen.backSideOrderPointer,
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
    color = LightMainPalettes.deckRepetitionScreen.frontSideCardWord,
)

val DarkFrontSideCardWordTextStyle = MainTextStyle.copy(
    fontSize = CommonDimension.cardWordTextSize,
    fontStyle = FontStyle.Italic,
    fontWeight = FontWeight.Bold,
    color = DarkMainPalettes.deckRepetitionScreen.frontSideCardWord,
)

val LightBackSideCardWordTextStyle = MainTextStyle.copy(
    fontSize = CommonDimension.cardWordTextSize,
    fontStyle = FontStyle.Italic,
    fontWeight = FontWeight.Bold,
    color = LightMainPalettes.deckRepetitionScreen.backSideCardWord,
)

val DarkBackSideCardWordTextStyle = MainTextStyle.copy(
    fontSize = CommonDimension.cardWordTextSize,
    fontStyle = FontStyle.Italic,
    fontWeight = FontWeight.Bold,
    color = DarkMainPalettes.deckRepetitionScreen.backSideCardWord,
)


val IpaPromptsTextStyle = MainTextStyle.copy(
    fontSize = CommonDimension.ipaPromptsTextSize,
    fontStyle = FontStyle.Italic
)

private val ViewingCardTextStyle =
    MainTextStyle.copy(fontSize = CommonDimension.viewingCardContentTextSize)

val CommonViewingCardDeckNameTextStyle = MainTextStyle.copy(
    fontSize = CommonDimension.viewingCardDeckNameTextSize,
    fontStyle = FontStyle.Italic,
    fontWeight = FontWeight.Bold
)

val LightViewingCardNativeWord = ViewingCardTextStyle

val LightViewingCardForeignWord = ViewingCardTextStyle.copy(
    color = LightMainPalettes.cardViewingScreen.foreignWord
)

val LightViewingCardIpa = ViewingCardTextStyle.copy(
    color = LightMainPalettes.cardViewingScreen.ipa
)

val LightViewingCardOrdinal = ViewingCardTextStyle.copy(
    color = LightMainPalettes.cardViewingScreen.ordinal,
    fontStyle = FontStyle.Italic
)

val DarkViewingCardNativeWord = ViewingCardTextStyle

val DarkViewingCardForeignWord = ViewingCardTextStyle.copy(
    color = DarkMainPalettes.cardViewingScreen.foreignWord
)

val DarkViewingCardIpa = ViewingCardTextStyle.copy(
    color = DarkMainPalettes.cardViewingScreen.ipa
)

val DarkViewingCardOrdinal = ViewingCardTextStyle.copy(
    color = DarkMainPalettes.cardViewingScreen.ordinal,
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
    color = LightMainPalettes.cardManagementView.foreignWord,
    fontWeight = FontWeight.ExtraBold
)

val DarkForeignWordAutocompleteSpanStyle = MainSpanStyle.copy(
    color = DarkMainPalettes.cardManagementView.foreignWord,
    fontWeight = FontWeight.ExtraBold
)

val LightIpaValueTextStyle = MainTextStyle.copy(color = LightMainPalettes.material.onPrimary)

val DarkIpaValueTextStyle = MainTextStyle.copy(color = DarkMainPalettes.material.onPrimary)

val commonDeckNavigationDialogTitle = MainTextStyle.copy(
    fontSize = 30.sp,
    fontStyle = FontStyle.Italic
)