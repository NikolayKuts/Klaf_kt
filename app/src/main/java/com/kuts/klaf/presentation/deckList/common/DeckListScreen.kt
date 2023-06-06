package com.kuts.klaf.presentation.deckList.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.kuts.domain.common.ScheduledDateState
import com.kuts.domain.common.isEven
import com.kuts.domain.entities.Deck
import com.kuts.klaf.R
import com.kuts.klaf.data.common.getScheduledDateStateByByCalculatedRange
import com.kuts.klaf.presentation.common.*
import com.kuts.klaf.presentation.theme.MainTheme

@Composable
fun DeckListScreen(
    decks: List<Deck>?,
    shouldSynchronizationIndicatorBeShown: Boolean,
    onRefresh: () -> Unit,
    onItemClick: (deck: Deck) -> Unit,
    onLongItemClick: (deck: Deck) -> Unit,
    onMainButtonClick: () -> Unit,
    onRestartApp: () -> Unit,
) {
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = false)
    val offsetY = swipeRefreshState.indicatorOffset
    var visible by rememberAsMutableStateOf(value = false)

    SwipeRefresh(
        modifier = Modifier.fillMaxSize(),
        state = swipeRefreshState,
        onRefresh = onRefresh,
        indicator = { _, _ ->
            SynchronizationRefreshingIndicator(
                visible = visible,
                offsetY = LocalDensity.current.run { offsetY.toDp() - 50.dp }
            )
        }
    ) {

        Box(modifier = Modifier.fillMaxSize()) {
            if (decks == null) {
                FetchingDecksWarningView(onRestartApp = onRestartApp)
            } else {
                DecksContentView(
                    decks = decks,
                    onItemClick = onItemClick,
                    onLongItemClick = onLongItemClick,
                    onMainButtonClick = onMainButtonClick,
                )

                AnimatableDataSynchronizationIndicator(
                    visible = shouldSynchronizationIndicatorBeShown,
                    modifier = Modifier.offset(y = LocalDensity.current.run { offsetY.toDp() })
                )
            }
        }
    }

    LaunchedEffect(key1 = swipeRefreshState.isSwipeInProgress) {
        visible = swipeRefreshState.isSwipeInProgress && !shouldSynchronizationIndicatorBeShown
    }
}

@Composable
private fun FetchingDecksWarningView(onRestartApp: () -> Unit) {
    FullBackgroundDialog(
        onBackgroundClick = {},
        topContent = ContentHolder(size = ROUNDED_ELEMENT_SIZE.dp) {
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
                background = MainTheme.colors.common.neutralDialogButton,
                iconId = R.drawable.ic_close_24,
                onClick = { onRestartApp() }
            )
        }
    )
}

@Composable
private fun SynchronizationRefreshingIndicator(
    visible: Boolean,
    offsetY: Dp,
) {
    if (visible) {
        Card(
            modifier = Modifier
                .size(ROUNDED_ELEMENT_SIZE.dp)
                .noRippleClickable { }
                .offset(y = offsetY),
            shape = RoundedCornerShape(ROUNDED_ELEMENT_SIZE.dp),
            elevation = 0.dp,
        ) {
            Icon(
                modifier = Modifier
                    .size(20.dp)
                    .background(MainTheme.colors.common.dialogBackground)
                    .padding(8.dp),
                painter = painterResource(id = R.drawable.ic_sync_24),
                contentDescription = null,
            )
        }
    }
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
        contentPadding = PaddingValues(bottom = 124.dp),
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
        background = MainTheme.colors.material.primary,
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
    onLongItemClick: (deck: Deck) -> Unit,
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
        MainTheme.colors.deckListScreen.lightDeckItemBackground
    } else {
        MainTheme.colors.deckListScreen.darkDeckItemBackground
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

@Composable
private fun BoxScope.AnimatableDataSynchronizationIndicator(
    visible: Boolean,
    modifier: Modifier,
) {
    val visibilityState = remember(visible) { MutableTransitionState(initialState = false) }
    val transitionDuration = 500
    val additionOffset = 50

    AnimatedVisibility(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(32.dp),
        visibleState = visibilityState,
        enter = slideInVertically(
            animationSpec = tween(durationMillis = transitionDuration),
            initialOffsetY = { fullWidth -> -(fullWidth + additionOffset) },
        ),
        exit = fadeOut(
            animationSpec = tween(durationMillis = 1),
        )
    ) {
        Card(
            modifier = modifier.align(Alignment.TopCenter),
            shape = RoundedCornerShape(size = ROUNDED_ELEMENT_SIZE.dp),
            contentColor = MainTheme.colors.material.onBackground,
            elevation = 4.dp
        ) {
            AnimatedSynchronizationLabel()
        }
    }

    LaunchedEffect(key1 = visible) {
        visibilityState.targetState = visible
    }
}