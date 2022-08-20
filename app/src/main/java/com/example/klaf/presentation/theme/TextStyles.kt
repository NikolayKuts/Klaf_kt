package com.example.klaf.presentation.theme

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

val DarkEvenDeckItemNameTextStyle = TextStyle(
    color = DarkMainPalettes.evenDeckItemName,
    fontSize = CommonDimension.deckItemDeckName,
)

val DarkOddDeckItemNameTextStyle = TextStyle(
    color = DarkMainPalettes.oddDeckItemName,
    fontSize = CommonDimension.deckItemDeckName,
)

val LightEvenDeckItemNameTextStyle = TextStyle(
    color = LightMainPalettes.evenDeckItemName,
    fontSize = CommonDimension.deckItemDeckName,
)

val LightOddDeckItemNameTextStyle = TextStyle(
    color = LightMainPalettes.oddDeckItemName,
    fontSize = CommonDimension.deckItemDeckName,
)

val DarkScheduledDateTextStyle = TextStyle(
    color = DarkMainPalettes.scheduledDate
)

val LightScheduledDateTextStyle = TextStyle(
    color = LightMainPalettes.scheduledDate
)

val DarkOverdueScheduledDateTextStyle = TextStyle(
    color = DarkMainPalettes.overdueScheduledDate
)

val LightOverdueScheduledDateTextStyle = TextStyle(
    color = LightMainPalettes.overdueScheduledDate
)

val DarkDeckItemRepetitionQuantitySpanStyle = SpanStyle(
    color = DarkMainPalettes.deckItemRepetitionQuantity
)

val LightDeckItemRepetitionQuantitySpanStyle = SpanStyle(
    color = LightMainPalettes.deckItemRepetitionQuantity
)

val DarkDeckItemCardQuantityTextStyle = SpanStyle(
    color = DarkMainPalettes.deckItemCardQuantity,
)

val LightDeckItemCardQuantityTextStyle = SpanStyle(
    color = LightMainPalettes.deckItemCardQuantity,
)

val DarkDeckItemPointerTextStyle = SpanStyle(
    color = DarkMainPalettes.deckItemPointer,
    fontSize = CommonDimension.deckItemPointer
)

val LightDeckItemPointerTextStyle = SpanStyle(
    color = LightMainPalettes.deckItemPointer,
    fontSize = CommonDimension.deckItemPointer
)

val DialogTextStyle = Typography.body1.copy(
    lineHeight = CommonDimension.dialogTextLineHeight
)

val AccentedDialogTextStyle = SpanStyle(
    fontStyle = FontStyle.Italic,
    fontWeight = FontWeight.Bold
)

val CardPointerTextStyle = SpanStyle(
    fontStyle = FontStyle.Italic,
    fontSize = CommonDimension.cardAdditionPointerTextSize
)

val CardPointerValueTextStyle = SpanStyle(
    fontWeight = FontWeight.Bold,
    fontStyle = FontStyle.Italic,
)

val LightFrondSideOrderPointer = TextStyle(
    color = LightMainPalettes.frontSideOrderPointer
)

val LightBackSideOrderPointer = TextStyle(
    color = LightMainPalettes.backSideOrderPointer
)

val DarkFrondSideOrderPointer = TextStyle(
    color = DarkMainPalettes.frontSideOrderPointer
)

val DarkBackSideOrderPointer = TextStyle(
    color = DarkMainPalettes.backSideOrderPointer
)

val TimerTextStile = TextStyle(
    fontSize = CommonDimension.timerTextSize,
    fontStyle = FontStyle.Italic
)

val CardWordTextStyle = Typography.body1.copy(
    fontSize = CommonDimension.cardWordTextSize,
    fontStyle = FontStyle.Italic
)

val IpaPromptsTextStyle = Typography.body1.copy(
    fontSize = CommonDimension.ipaPromptsTextSize,
    fontStyle = FontStyle.Italic
)