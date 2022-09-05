package com.example.klaf.presentation.deckList.common

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.klaf.presentation.common.RoundButton
import com.example.klaf.presentation.theme.MainTheme
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

@Composable
fun DeckListScreen(
    viewModel: DeckListViewModel,
    onItemClick: (deck: Deck) -> Unit,
    onLongItemClick: (deck: Deck) -> Unit,
    onMainButtonClick: () -> Unit,
    onSwipeRefresh: () -> Unit,
) {
    val decks by viewModel.deckSource.collectAsState()

    SwipeRefresh(
        modifier = Modifier.fillMaxSize(),
        state = rememberSwipeRefreshState(isRefreshing = false),
        onRefresh = onSwipeRefresh
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 124.dp)

            ) {
                itemsIndexed(decks) { index, deck ->
                    DeckItemView(
                        deck = deck,
                        position = index,
                        onItemClick = onItemClick,
                        onLongItemClick = onLongItemClick
                    )
                }
            }
            RoundButton(
                background = MainTheme.colors.materialColors.primary,
                iconId = R.drawable.ic_add_24,
                onClick = onMainButtonClick,
                modifier = Modifier
                    .align(alignment = Alignment.BottomEnd)
                    .padding(bottom = 48.dp, end = 48.dp),
                elevation = 4.dp
            )
        }
    }
}

@Suppress("OPT_IN_IS_NOT_ENABLED")
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeckItemView(
    deck: Deck,
    position: Int,
    onItemClick: (Deck) -> Unit,
    onLongItemClick: (Deck) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(2.dp)
            .combinedClickable(
                onClick = { onItemClick(deck) },
                onLongClick = { onLongItemClick(deck) }
            ),
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
        modifier = Modifier.weight(0.9F),
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