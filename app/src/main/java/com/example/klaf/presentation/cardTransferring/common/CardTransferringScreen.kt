package com.example.klaf.presentation.cardTransferring.common

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.LazyGridItemScope.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.common.ifTrue
import com.example.klaf.R
import com.example.klaf.presentation.cardTransferring.common.CardTransferringNavigationEvent.*
import com.example.klaf.presentation.common.*
import com.example.klaf.presentation.theme.MainTheme
import kotlinx.coroutines.delay

private const val CLOSING_ANIMATION_DELAY = 500L

@Composable
fun CardTransferringScreen(viewModel: BaseCardTransferringViewModel) {
    val deck = viewModel.sourceDeck.collectAsState(initial = null).value ?: return
    val cardHolders by viewModel.cardHolders.collectAsState()
    var moreButtonClickedState by rememberAsMutableStateOf(value = true)

    val density = LocalDensity.current
    var parentHeightPx by rememberAsMutableStateOf(value = 0F)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { parentHeightPx = it.height.toFloat() },
    ) {
        item {
            val (
                contentHeightPx: Float,
                buttonGroupPadding: Dp,
                bottomContentPadding: Dp,
            ) = getScreenParams(
                parentHeightPx = parentHeightPx,
                minContentHeightPx = density.run { 400.dp.toPx() }
            )

            Box(
                modifier = Modifier
                    .height(density.run { contentHeightPx.toDp() })
                    .fillMaxWidth()
                    .padding(28.dp)
            ) {
                Column {
                    Header(deckName = deck.name)

                    QuantityPointers(
                        totalValue = deck.cardQuantity.toString(),
                        selectedValue = cardHolders.filter { it.isSelected }.size.toString()
                    )

                    Spacer(modifier = Modifier.height(28.dp))
                    DividingLine()

                    DeckList(
                        moreButtonClickedState = moreButtonClickedState,
                        cardHolders = cardHolders,
                        bottomContentPadding = bottomContentPadding,
                        onScroll = { moreButtonClickedState = false },
                        onSelectedChanged = { index ->
                            viewModel.changeSelectionState(position = index)
                            moreButtonClickedState = false
                        },
                        onLongItemClick = { index ->
                            viewModel.navigate(event = ToCardEditingFragment(cardSelectionIndex = index))
                        }
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(buttonGroupPadding),
                ) {
                    ManagementButtons(
                        clickState = moreButtonClickedState,
                        onMoveCardsClick = { viewModel.navigate(event = ToCardMovingDialog) },
                        onAddCardsClick = { viewModel.navigate(event = ToCardAddingFragment) },
                        onDeleteCardsClick = { viewModel.navigate(event = ToCardDeletionDialog) },
                        onMoreButtonClick = { moreButtonClickedState = !moreButtonClickedState }
                    )
                }

                LaunchedEffect(key1 = null) {
                    delay(CLOSING_ANIMATION_DELAY)
                    moreButtonClickedState = false
                }
            }
        }
    }
}

private fun getScreenParams(
    parentHeightPx: Float,
    minContentHeightPx: Float
): Triple<Float, Dp, Dp> {
    return if (
        parentHeightPx > 0F
        && parentHeightPx < minContentHeightPx
    ) {
        Triple(
            first = minContentHeightPx,
            second = 0.dp,
            third = 60.dp
        )
    } else {
        Triple(
            first = parentHeightPx,
            second = 16.dp,
            third = 90.dp
        )
    }
}

@Composable
private fun ColumnScope.Header(deckName: String) {
    Text(
        modifier = Modifier
            .align(alignment = Alignment.CenterHorizontally)
            .padding(top = 8.dp, bottom = 8.dp),
        text = deckName,
        style = MainTheme.typographies.cardTransferringScreenTextStyles.header
    )
}

@Composable
private fun QuantityPointers(
    totalValue: String,
    selectedValue: String,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                .background(MainTheme.colors.cardTransferringScreen.quantityPointerBackground)
                .padding(start = 4.dp, end = 4.dp),
            text = stringResource(R.string.pointer_interim_deck_cards),
            style = MainTheme.typographies.cardTransferringScreenTextStyles.pointerTitle,
        )
        Spacer(modifier = Modifier.height(8.dp))

        QuantityPointer(
            pointerText = stringResource(R.string.pointer_interim_deck_total),
            pointerValue = totalValue
        )

        QuantityPointer(
            pointerText = stringResource(R.string.pointer_interim_deck_selected),
            pointerValue = selectedValue
        )
    }
}

@Composable
private fun QuantityPointer(
    pointerText: String,
    pointerValue: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = pointerText)
        Text(
            text = pointerValue,
            style = MainTheme.typographies.cardTransferringScreenTextStyles.quantityPointerValue,
        )
    }
}

@Composable
private fun DeckList(
    moreButtonClickedState: Boolean,
    cardHolders: List<SelectableCardHolder>,
    bottomContentPadding: Dp,
    onScroll: () -> Unit,
    onSelectedChanged: (index: Int) -> Unit,
    onLongItemClick: (index: Int) -> Unit,
) {
    val lazyListState = rememberLazyListState()

    LazyColumn(
        state = lazyListState,
        contentPadding = PaddingValues(bottom = bottomContentPadding),
        modifier = Modifier.scrollable(
            orientation = Orientation.Vertical,
            state = rememberScrollableState { delta ->
                moreButtonClickedState.ifTrue { onScroll() }
                delta
            }
        )
    ) {
        itemsIndexed(
            items = cardHolders,
            key = { _, holder -> holder.card.id },
        ) { index, holder ->
            CardItem(
                holder = holder,
                position = index + 1,
                onSelectedChanged = { onSelectedChanged(index) },
                onLongClick = { onLongItemClick(index) }
            )
            DividingLine()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LazyItemScope.CardItem(
    holder: SelectableCardHolder,
    position: Int,
    onSelectedChanged: (Boolean) -> Unit,
    onLongClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .animateItemPlacement()
            .combinedClickable(
                onClick = {},
                onLongClick = onLongClick
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = position.toString(),
            color = MainTheme.colors.cardTransferringScreen.cardOrdinal,
            fontStyle = FontStyle.Italic
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = holder.card.foreignWord,
            color = MainTheme.colors.cardTransferringScreen.foreignWord
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            modifier = Modifier.weight(1F),
            text = holder.card.nativeWord,
        )

        CustomCheckBox(
            modifier = Modifier.padding(6.dp),
            checked = holder.isSelected,
            onCheckedChange = onSelectedChanged,
            uncheckedBorderColor = MainTheme.colors.cardTransferringScreen.unCheckedBorder,
            checkedBoxColor = MainTheme.colors.cardTransferringScreen.selectedCheckBox
        )
    }
}

@Composable
private fun DividingLine() {
    val padding = 4.dp
    Divider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = padding, bottom = padding),
        color = MainTheme.colors.cardTransferringScreen.itemDivider,
    )
}

@Composable
private fun BoxScope.ManagementButtons(
    clickState: Boolean,
    onMoveCardsClick: () -> Unit,
    onAddCardsClick: () -> Unit,
    onDeleteCardsClick: () -> Unit,
    onMoreButtonClick: () -> Unit,
) {
    Box(
        modifier = Modifier.align(Alignment.BottomEnd),
        contentAlignment = Alignment.BottomCenter
    ) {
        CardTransferringButton(isStartPosition = clickState, onClick = onMoveCardsClick)
        CardAddingButton(isStartPosition = clickState, onClick = onAddCardsClick)
        CardDeletingButton(isStartPosition = clickState, onClick = onDeleteCardsClick)
        MoreButton(clicked = clickState, onClick = onMoreButtonClick)
    }
}

@Composable
private fun CardTransferringButton(
    isStartPosition: Boolean,
    onClick: () -> Unit,
) {
    AnimatableOffsetButton(
        isStartPosition = isStartPosition,
        offset = -180F,
        background = MainTheme.colors.cardTransferringScreen.transferringButton,
        iconId = R.drawable.ic_move_24,
        stiffness = Spring.StiffnessVeryLow,
        onClick = onClick
    )
}

@Composable
private fun CardAddingButton(
    isStartPosition: Boolean,
    onClick: () -> Unit,
) {
    AnimatableOffsetButton(
        isStartPosition = isStartPosition,
        offset = -120F,
        background = MainTheme.colors.cardTransferringScreen.cardAddingButton,
        iconId = R.drawable.ic_add_24,
        stiffness = Spring.StiffnessLow,
        onClick = onClick,
    )
}

@Composable
private fun CardDeletingButton(
    isStartPosition: Boolean,
    onClick: () -> Unit,
) {
    AnimatableOffsetButton(
        isStartPosition = isStartPosition,
        offset = -60F,
        background = MainTheme.colors.cardTransferringScreen.deletingButton,
        iconId = R.drawable.ic_delete_24,
        stiffness = Spring.StiffnessMediumLow,
        onClick = onClick,
    )
}

@Composable
private fun AnimatableOffsetButton(
    isStartPosition: Boolean,
    offset: Float,
    background: Color,
    @DrawableRes iconId: Int,
    onClick: () -> Unit,
    rotationDegrees: Float = 180F,
    stiffness: Float = Spring.StiffnessVeryLow,
    elevation: Dp = 4.dp,
) {
    val transition = updateTransition(targetState = isStartPosition, label = null)

    val xOffset by transition.animateFloat(
        transitionSpec = {
            spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = stiffness)
        },
        label = ""
    ) { state -> if (state) offset else 0F }

    val alpha by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 800) },
        label = ""
    ) { if (it) 1F else 0F }

    val degrees by transition.animateFloat(
        transitionSpec = {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessVeryLow
            )
        },
        label = ""
    ) { state -> if (state) 0F else rotationDegrees }

    RoundButton(
        modifier = Modifier
            .offset(y = xOffset.dp)
            .alpha(alpha)
            .rotate(degrees = degrees)
            .padding(10.dp),
        background = background,
        iconId = iconId,
        onClick = onClick,
        elevation = elevation
    )
}

@Composable
private fun MoreButton(
    clicked: Boolean,
    onClick: () -> Unit,
) {
    val color by animateColorAsState(
        targetValue = if (!clicked) {
            MainTheme.colors.cardTransferringScreen.clickedMoreButton
        } else {
            MainTheme.colors.cardTransferringScreen.unClickedMoreButton
        }
    )
    val scale by animateFloatAsState(
        targetValue = if (clicked) 0.9F else 1.0F,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy)
    )

    RoundButton(
        modifier = Modifier
            .scale(scale)
            .padding(10.dp),
        background = color,
        iconId = R.drawable.ic_more_vert_24,
        onClick = onClick
    )
}