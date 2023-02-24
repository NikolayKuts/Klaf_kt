package com.example.klaf.presentation.deckList.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.domain.common.ScheduledDateState
import com.example.domain.common.isEven
import com.example.domain.entities.Deck
import com.example.klaf.R
import com.example.klaf.data.common.getScheduledDateStateByByCalculatedRange
import com.example.klaf.presentation.common.FullBackgroundDialog
import com.example.klaf.presentation.common.RoundButton
import com.example.klaf.presentation.common.RoundedIcon
import com.example.klaf.presentation.common.rememberAsMutableStateOf
import com.example.klaf.presentation.theme.MainTheme
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

@Composable
fun DeckListScreen(
    viewModel: BaseDeckListViewModel,
    onMainButtonClick: () -> Unit,
    onSwipeRefresh: () -> Unit,
    onRestartApp: () -> Unit,
) {
    SwipeRefresh(
        modifier = Modifier.fillMaxSize(),
        state = rememberSwipeRefreshState(isRefreshing = false),
        onRefresh = onSwipeRefresh
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val decks by viewModel.deckSource.collectAsState()

            if (decks == null) {
                FetchingDecksWarningView(onRestartApp = onRestartApp)
            } else {
                DecksContentView(
                    decks = decks ?: emptyList(),
                    onItemClick = {
                        viewModel.navigate(event = DeckListNavigationEvent.ToFragment(deck = it))
                    },
                    onLongItemClick = {
                        viewModel.navigate(
                            event = DeckListNavigationEvent.ToDeckNavigationDialog(deck = it)
                        )
                    },
                    onMainButtonClick = onMainButtonClick
                )
            }
        }
    }
}

@Composable
private fun FetchingDecksWarningView(onRestartApp: () -> Unit) {
    FullBackgroundDialog(
        onBackgroundClick = {},
        topContent = {
            RoundedIcon(background = Color.Transparent, iconId = R.drawable.ic_sad_face_24)
        },
        mainContent = {
            Text(
                text = stringResource(id = R.string.problem_fetching_decks_view_message),
                textAlign = TextAlign.Center
            )
        },
        bottomContent = {
            RoundButton(
                background = MainTheme.colors.neutralDialogButton,
                iconId = R.drawable.ic_close_24,
                onClick = { onRestartApp() }
            )
        }
    )
}

@Composable
private fun BoxScope.DecksContentView(
    decks: List<Deck>,
    onItemClick: (deck: Deck) -> Unit,
    onLongItemClick: (deck: Deck) -> Unit,
    onMainButtonClick: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 124.dp)
    ) {
        itemsIndexed(
            items = decks,
            key = { _, deck -> deck.id }
        ) { index, deck ->
            DeckItemView(
                deck = deck,
                position = index,
                onItemClick = onItemClick,
                onLongItemClick = onLongItemClick,
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

@Suppress("OPT_IN_IS_NOT_ENABLED")
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LazyItemScope.DeckItemView(
    deck: Deck,
    position: Int,
    onItemClick: (deck: Deck) -> Unit,
    onLongItemClick: (deck: Deck) -> Unit
) {
    var animationFloat by rememberAsMutableStateOf(value = 0F)
    val animationFloatState by animateFloatAsState(
        targetValue = animationFloat,
        animationSpec = tween(durationMillis = 150 + position * 5)
    )

    LaunchedEffect(key1 = null) { animationFloat = 1F }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(2.dp)
            .animateItemPlacement()
            .scale(animationFloatState)
            .alpha(animationFloatState)
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
            Spacer(modifier = Modifier.width(16.dp))
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ScheduledDateView(deck = deck)
                ItemContentSpacer()
                RepetitionQuantityView(deck = deck)
                ItemContentSpacer()
                CardQuantityView(deck = deck)
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
private fun ItemContentSpacer() {
    Spacer(modifier = Modifier.width(8.dp))
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