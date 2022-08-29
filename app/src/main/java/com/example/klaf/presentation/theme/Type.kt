package com.example.klaf.presentation.theme

import androidx.compose.material.Typography
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle

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
)

val Typography = Typography(
    body1 = Body1,
    caption = Caption,
    subtitle1 = Subtitle1,
    button = Button,
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
)