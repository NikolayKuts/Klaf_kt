package com.example.klaf.presentation.theme

import androidx.compose.material.Typography
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

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
    val frontSideCardWordTextStyle: TextStyle,
    val backSideCardWordTextStyle: TextStyle,
    val cardIpaPromptsTextStyle: TextStyle,
    val viewingCardDeckName: TextStyle,
    val viewingCardNativeWord: TextStyle,
    val viewingCardForeignWord: TextStyle,
    val viewingCardIpa: TextStyle,
    val viewingCardOrdinal: TextStyle,
    val deckRepetitionInfoScreenTextStyles: DeckRepetitionInfoScreenTextStyles,
    val cardTransferringScreenTextStyles: CardTransferringScreenTextStyles,
)

data class DeckRepetitionInfoScreenTextStyles(
    val pointer: TextStyle,
    val deckName: TextStyle,
)

data class CardTransferringScreenTextStyles(
    val header: TextStyle,
    val pointerTitle: TextStyle,
    val quantityPointerValue: TextStyle,
    val choosingContent: TextStyle,
)

private val Typography = Typography(
    body1 = Body1,
    caption = Caption,
    subtitle1 = Subtitle1,
    button = Button,
)

private val CommonDeckRepetitionInfoScreenTextStyles = DeckRepetitionInfoScreenTextStyles(
    pointer = Body1.copy(fontStyle = FontStyle.Italic),
    deckName = Body1.copy(
        fontStyle = FontStyle.Italic,
        fontSize = CommonDimension.deckRepetitionInfoScreenDimensions.deckName,
        fontWeight = FontWeight.Bold
    )
)


val LightMainTypographies = MainTopographies(
    materialTypographies = Typography,
    evenDeckItemName = LightEvenDeckItemNameTextStyle,
    oddDeckItemName = LightOddDeckItemNameTextStyle,
    scheduledDateRange = LightDeckItemScheduledDateTextStyle,
    overdueScheduledDateRange = LightDeckItemOverdueScheduledDateTextStyle,
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
    frontSideCardWordTextStyle = LightFrontSideCardWordTextStyle,
    backSideCardWordTextStyle = LightBackSideCardWordTextStyle,
    cardIpaPromptsTextStyle = IpaPromptsTextStyle,
    viewingCardDeckName = CommonViewingCardDeckNameTextStyle,
    viewingCardNativeWord = LightViewingCardNativeWord,
    viewingCardForeignWord = LightViewingCardForeignWord,
    viewingCardIpa = LightViewingCardIpa,
    viewingCardOrdinal = LightViewingCardOrdinal,
    deckRepetitionInfoScreenTextStyles = CommonDeckRepetitionInfoScreenTextStyles,
    cardTransferringScreenTextStyles = CannonCardTransferringScreenTextStyles,
)

val DarkMainTypographies = MainTopographies(
    materialTypographies = Typography,
    evenDeckItemName = DarkEvenDeckItemNameTextStyle,
    oddDeckItemName = DarkOddDeckItemNameTextStyle,
    scheduledDateRange = DarkDeckItemScheduledDateTextStyle,
    overdueScheduledDateRange = DarkDeckItemOverdueScheduledDateTextStyle,
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
    frontSideCardWordTextStyle = DarkFrontSideCardWordTextStyle,
    backSideCardWordTextStyle = DarkBackSideCardWordTextStyle,
    cardIpaPromptsTextStyle = IpaPromptsTextStyle,
    viewingCardDeckName = CommonViewingCardDeckNameTextStyle,
    viewingCardNativeWord = DarkViewingCardNativeWord,
    viewingCardForeignWord = DarkViewingCardForeignWord,
    viewingCardIpa = DarkViewingCardIpa,
    viewingCardOrdinal = DarkViewingCardOrdinal,
    deckRepetitionInfoScreenTextStyles = CommonDeckRepetitionInfoScreenTextStyles,
    cardTransferringScreenTextStyles = CannonCardTransferringScreenTextStyles,
)