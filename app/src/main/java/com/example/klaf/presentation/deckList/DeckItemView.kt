package com.example.klaf.presentation.deckList

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.klaf.R
import com.example.klaf.domain.common.ScheduledDateState
import com.example.klaf.domain.common.getScheduledDateStateByByCalculatedRange
import com.example.klaf.domain.common.isEven
import com.example.klaf.domain.entities.Deck
import com.example.klaf.presentation.theme.MainTheme

@Composable
fun DeckItemView(
    deck: Deck,
    position: Int,
    onItemClick: (Deck) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(2.dp)
            .clickable { onItemClick(deck) },
        backgroundColor = getCardBackgroundColorByPosition(position),
        elevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DeckNameView(deckName = deck.name, position = position)
            Spacer(modifier = Modifier.width(4.dp))
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ScheduledDateView(deck = deck)
                Spacer(modifier = Modifier.width(4.dp))
                RepetitionQuantityView(deck = deck)
                Spacer(modifier = Modifier.width(4.dp))
                CardQuantityView(deck = deck)
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
    }
}

@Composable
private fun RowScope.DeckNameView(deckName: String, position: Int) {
    Text(
        modifier = Modifier
            .weight(0.9F),
        style = getDeckNameStyleByPosition(position),
        text = deckName,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun ScheduledDateView(deck: Deck) {
    val scheduledDateState =
        deck.getScheduledDateStateByByCalculatedRange(context = LocalContext.current)

    Text(
        text = scheduledDateState.range,
        style = getScheduledDateStyleByScheduledDateState(state = scheduledDateState)
    )
}

@Composable
private fun RepetitionQuantityView(deck: Deck) {
    Text(
        text = buildAnnotatedString {
            withStyle(style = MainTheme.typographies.deckItemRepetitionQuantity) {
                append(deck.repetitionQuantity.toString())
            }
            withStyle(style = MainTheme.typographies.deckItemPointer) {
                append(stringResource(R.string.repetition_quantity_pointer))
            }
        }
    )
}

@Composable
private fun CardQuantityView(deck: Deck) {
    Text(
        text = buildAnnotatedString {
            withStyle(style = MainTheme.typographies.deckItemCardQuantity) {
                append(deck.cardQuantity.toString())
            }
            withStyle(style = MainTheme.typographies.deckItemPointer) {
                append(stringResource(R.string.card_quantity_pointer))
            }
        }
    )
}

@Composable
private fun getCardBackgroundColorByPosition(position: Int): Color {
    return if (position.isEven()) {
        MainTheme.colors.lightDeckItemBackground
    } else {
        MainTheme.colors.darkDeckItemBackground
    }
}

@Composable
private fun getDeckNameStyleByPosition(position: Int): TextStyle {
    return if (position.isEven()) {
        MainTheme.typographies.evenDeckItemName
    } else {
        MainTheme.typographies.oddDeckItemName
    }
}

@Composable
private fun getScheduledDateStyleByScheduledDateState(state: ScheduledDateState): TextStyle {
    return if (state.isOverdue) {
        MainTheme.typographies.overdueScheduledDateRange
    } else {
        MainTheme.typographies.scheduledDateRange
    }
}
