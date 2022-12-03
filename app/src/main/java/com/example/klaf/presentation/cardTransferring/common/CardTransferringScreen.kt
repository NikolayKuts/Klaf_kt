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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.*

import androidx.compose.foundation.lazy.grid.LazyGridItemScope.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.klaf.R
import com.example.klaf.domain.common.ifTrue
import com.example.klaf.presentation.cardTransferring.common.CardTransferringNavigationEvent.*
import com.example.klaf.presentation.common.CustomCheckBox
import com.example.klaf.presentation.common.RoundButton
import com.example.klaf.presentation.common.rememberAsMutableStateOf
import com.example.klaf.presentation.theme.MainTheme
import kotlinx.coroutines.delay

private const val CLOSING_ANIMATION_DELAY = 500L

@Composable
fun CardTransferringScreen(viewModel: BaseCardTransferringViewModel) {
    val deck = viewModel.sourceDeck.collectAsState(initial = null).value ?: return
    val cardHolders by viewModel.cardHolders.collectAsState()
    var moreButtonClickedState by rememberAsMutableStateOf(value = true)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp)
        ) {
            Header()

            QuantityPointers(
                totalValue = deck.cardQuantity.toString(),
                selectedValue = cardHolders.filter { it.isSelected }.size.toString()
            )

            Spacer(modifier = Modifier.height(28.dp))
            DividingLine()

            DeckList(
                moreButtonClickedState = moreButtonClickedState,
                cardHolders = cardHolders,
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

@Composable
private fun ColumnScope.Header() {
    Text(
        modifier = Modifier
            .align(alignment = Alignment.CenterHorizontally)
            .padding(top = 8.dp, bottom = 8.dp),
        text = stringResource(id = R.string.Interim_deck_name),
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
                .background(MainTheme.colors.cardTransferringScreenColors.quantityPointerBackground)
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
    onScroll: () -> Unit,
    onSelectedChanged: (index: Int) -> Unit,
    onLongItemClick: (index: Int) -> Unit
) {
    val scrollState = rememberLazyListState()

    LazyColumn(
        state = scrollState,
        contentPadding = PaddingValues(bottom = 100.dp),
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
    onLongClick: () -> Unit
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
            color = MainTheme.colors.cardTransferringScreenColors.cardOrdinal,
            fontStyle = FontStyle.Italic
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = holder.card.foreignWord,
            color = MainTheme.colors.cardTransferringScreenColors.foreignWord
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
            uncheckedBorderColor = MainTheme.colors.cardTransferringScreenColors.unCheckedBorder,
            checkedBoxColor = MainTheme.colors.cardTransferringScreenColors.selectedCheckBox
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
        color = MainTheme.colors.cardTransferringScreenColors.itemDivider,
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
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 48.dp, bottom = 48.dp),
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
        background = MainTheme.colors.cardTransferringScreenColors.transferringButton,
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
        background = MainTheme.colors.cardTransferringScreenColors.cardAddingButton,
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
        background = MainTheme.colors.cardTransferringScreenColors.deletingButton,
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
            MainTheme.colors.cardTransferringScreenColors.clickedMoreButton
        } else {
            MainTheme.colors.cardTransferringScreenColors.unClickedMoreButton
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