package com.kuts.klaf.presentation.deckRepetition

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kuts.domain.common.CardRepetitionOrder
import com.kuts.domain.common.CardSide
import com.kuts.domain.common.DeckRepetitionState
import com.kuts.domain.common.ifTrue
import com.kuts.domain.enums.DifficultyRecallingLevel.EASY
import com.kuts.domain.enums.DifficultyRecallingLevel.GOOD
import com.kuts.domain.enums.DifficultyRecallingLevel.HARD
import com.kuts.domain.ipa.LetterInfo
import com.kuts.domain.ipa.toIpaPrompts
import com.kuts.klaf.presentation.R
import com.kuts.klaf.presentation.common.ButtonState
import com.kuts.klaf.presentation.common.ContentHolder
import com.kuts.klaf.presentation.common.DIALOG_APP_LABEL_SIZE
import com.kuts.klaf.presentation.common.DialogAppLabel
import com.kuts.klaf.presentation.common.FullBackgroundDialog
import com.kuts.klaf.presentation.common.Pointer
import com.kuts.klaf.presentation.common.RoundButton
import com.kuts.klaf.presentation.common.ScrollableBox
import com.kuts.klaf.presentation.common.TimerCountingState
import com.kuts.klaf.presentation.common.timeAsString
import com.kuts.klaf.presentation.theme.MainTheme
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.launch

@Composable
fun DeckReviewScreen(
    viewModel: BaseDeckReviewViewModel,
    onDeleteCardClick: (cardId: Int) -> Unit,
    onAddCardClick: () -> Unit,
    onEditCardClick: (cardId: Int) -> Unit,
) {
    val deckRepetitionState by viewModel.cardState.collectAsState(initial = null)
    val deck by viewModel.deck.collectAsState(initial = null)
    val mainButtonState by viewModel.mainButtonState.collectAsState()
    val screenState by viewModel.screenState.collectAsState(RepetitionScreenState.StartState)

    val repetitionState = deckRepetitionState ?: return
    val receivedDeck = deck ?: return

    val density = LocalDensity.current
    val minContentHeightPx = density.run { 400.dp.toPx() }

    var shouldInterceptBack by remember { mutableStateOf(true) }
    var showExitDialog by remember { mutableStateOf(false) }

    val deckReviewState by viewModel.deckReviewState.collectAsState()

    ScrollableBox { parentHeightPx ->
        val contentHeight = when {
            parentHeightPx < minContentHeightPx -> minContentHeightPx
            else -> parentHeightPx
        }

        Row {
            Text(text = "reviewed: ${deckReviewState.reviewedCardsCount}")

            Spacer(Modifier.width(10.dp))

            Text(text = "max time: ${deckReviewState.maxTime.timeAsString}")

            Spacer(Modifier.width(10.dp))

            Text(text = "left: ${deckReviewState.leftTime.timeAsString}")
        }

        Box(
            modifier = Modifier
                .fillParentMaxWidth()
                .height(density.run { contentHeight.toDp() })
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DeckInfo(
                    deckName = receivedDeck.name,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.size(8.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    OrderPointers(
                        order = repetitionState.repetitionOrder,
                        onSwitchIconClick = { viewModel.changeRepetitionOrder() },
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
                    Timer(
                        viewModel = viewModel,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                DeckCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 8.dp),
                    deckRepetitionState = repetitionState,
                    onWordClick = { viewModel.pronounceWord() },
                )

                RepetitionButtons(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    deckRepetitionState = repetitionState,
                    screenState = screenState,
                    onStartButtonClick = { viewModel.startRepeating() },
                    onEasyButtonClick = { viewModel.moveCardByDifficultyRecallingLevel(level = EASY) },
                    onGoodButtonClick = { viewModel.moveCardByDifficultyRecallingLevel(level = GOOD) },
                    onHardButtonClick = { viewModel.moveCardByDifficultyRecallingLevel(level = HARD) },
                    onCardButtonClick = { viewModel.turnCard() },
                )
            }

            AdditionalButtons(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 32.dp),
                additionalButtonsEnabled = mainButtonState == ButtonState.PRESSED,
                onDeleteClick = {
                    repetitionState.card?.id?.let { cardId -> onDeleteCardClick(cardId) }
                },
                onAddClick = onAddCardClick,
                onEditClick = {
                    repetitionState.card?.id?.let { cardId -> onEditCardClick(cardId) }
                },
                onCommonButtonClick = { viewModel.changeButtonsStateOnCommonButtonClick() }
            )
        }
    }

    showExitDialog.ifTrue {
        ExitDialog(
            onDismiss = { showExitDialog = false },
            onConfirm = { shouldInterceptBack = false }
        )
    }

    BackHandler(enabled = shouldInterceptBack && screenState == RepetitionScreenState.RepetitionState) {
        showExitDialog = showExitDialog.not()
    }

    LaunchedEffect(key1 = showExitDialog) {
        if (showExitDialog) {
            viewModel.pauseTimerCounting()
        } else {
            viewModel.resumeTimerCounting()
        }
    }
}

@Composable
private fun DeckInfo(deckName: String, modifier: Modifier = Modifier) {
    Pointer(
        modifier = modifier,
        pointerTextId = R.string.pointer_deck,
        valueText = deckName
    )
}

@Composable
private fun OrderPointers(
    order: CardRepetitionOrder,
    onSwitchIconClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val frontSidePointerText = when (order) {
        CardRepetitionOrder.NATIVE_TO_FOREIGN -> stringResource(id = R.string.pointer_native)
        CardRepetitionOrder.FOREIGN_TO_NATIVE -> stringResource(id = R.string.pointer_foreign)
    }
    val backSidePointerText = when (order) {
        CardRepetitionOrder.NATIVE_TO_FOREIGN -> stringResource(id = R.string.pointer_foreign)
        CardRepetitionOrder.FOREIGN_TO_NATIVE -> stringResource(id = R.string.pointer_native)
    }

    Row(modifier = modifier) {
        Text(
            text = frontSidePointerText,
            style = MainTheme.typographies.frontSideOrderPointer
        )

        Icon(
            modifier = Modifier
                .padding(start = 8.dp, end = 4.dp)
                .clip(shape = RoundedCornerShape(50.dp))
                .clickable { onSwitchIconClick() },
            painter = painterResource(id = R.drawable.ic_rotate_24),
            contentDescription = null
        )
        Text(
            text = backSidePointerText,
            style = MainTheme.typographies.backSideOrderPointer
        )
    }
}

@Composable
private fun Timer(
    viewModel: BaseDeckReviewViewModel,
    modifier: Modifier = Modifier
) {
    val timerState by viewModel.timer.timerState.collectAsState()
    val timerColor = when (timerState.countingState) {
        TimerCountingState.RUN -> MainTheme.colors.deckRepetitionScreen.timerActive
        else -> MainTheme.colors.deckRepetitionScreen.timerInactive
    }

    Text(
        modifier = modifier,
        text = timerState.time,
        color = timerColor,
        style = MainTheme.typographies.timerTextStyle
    )
}

@Composable
private fun DeckCard(
    deckRepetitionState: DeckRepetitionState,
    onWordClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val card = deckRepetitionState.card ?: return
    val word: String
    var ipaPrompt = emptyList<LetterInfo>()

    when (deckRepetitionState.repetitionOrder) {
        CardRepetitionOrder.NATIVE_TO_FOREIGN -> {
            when (deckRepetitionState.side) {
                CardSide.FRONT -> {
                    word = card.nativeWord
                    ipaPrompt = emptyList()
                }

                CardSide.BACK -> {
                    word = card.foreignWord
                    ipaPrompt = card.toIpaPrompts()
                }
            }
        }

        CardRepetitionOrder.FOREIGN_TO_NATIVE -> {
            when (deckRepetitionState.side) {
                CardSide.FRONT -> {
                    word = card.foreignWord
                    ipaPrompt = card.toIpaPrompts()
                }

                CardSide.BACK -> {
                    word = card.nativeWord
                    ipaPrompt = emptyList()
                }
            }
        }
    }

    val wordTextStyle = when (deckRepetitionState.side) {
        CardSide.FRONT -> MainTheme.typographies.frontSideCardWordTextStyle
        CardSide.BACK -> MainTheme.typographies.backSideCardWordTextStyle
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            modifier = Modifier.clickable { onWordClick() },
            text = word,
            style = wordTextStyle,
        )

        LazyRow {
            itemsIndexed(items = ipaPrompt) { _, letterInfo ->
                val promptColor = when {
                    letterInfo.isChecked -> MainTheme.colors.deckRepetitionScreen.ipaPromptChecked
                    else -> MainTheme.colors.deckRepetitionScreen.ipaPromptUnchecked
                }

                Text(
                    text = letterInfo.letter,
                    color = promptColor,
                    style = MainTheme.typographies.cardIpaPromptsTextStyle
                )
            }
        }
    }
}

@Composable
private fun RepetitionButtons(
    deckRepetitionState: DeckRepetitionState,
    screenState: RepetitionScreenState,
    onStartButtonClick: () -> Unit,
    onEasyButtonClick: () -> Unit,
    onGoodButtonClick: () -> Unit,
    onHardButtonClick: () -> Unit,
    onCardButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (screenState) {
        RepetitionScreenState.StartState -> {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                RepetitionButton(textResId = R.string.start, onClick = onStartButtonClick)
            }
        }

        RepetitionScreenState.RepetitionState -> {
            Column(
                modifier = modifier,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CardButton(
                    modifier = Modifier.align(Alignment.End),
                    cardSide = deckRepetitionState.side,
                    onClick = onCardButtonClick,
                )
                Spacer(modifier = Modifier.size(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RepetitionButton(textResId = R.string.hard, onClick = onHardButtonClick)
                    RepetitionButton(textResId = R.string.good, onClick = onGoodButtonClick)
                    RepetitionButton(textResId = R.string.easy, onClick = onEasyButtonClick)
                }
            }
        }

        is RepetitionScreenState.FinishState -> {}
    }
}

@Composable
private fun RepetitionButton(
    @StringRes textResId: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        modifier = modifier,
        onClick = onClick,
    ) {
        Text(text = stringResource(textResId))
    }
}

@Composable
private fun AdditionalButtons(
    additionalButtonsEnabled: Boolean,
    onDeleteClick: () -> Unit,
    onAddClick: () -> Unit,
    onEditClick: () -> Unit,
    onCommonButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        DeleteButton(
            modifier = Modifier.align(Alignment.TopEnd),
            enabled = additionalButtonsEnabled,
            onClick = onDeleteClick
        )
        AddButton(
            modifier = Modifier.align(Alignment.TopEnd),
            enabled = additionalButtonsEnabled,
            onClick = onAddClick
        )
        EditButton(
            modifier = Modifier.align(Alignment.TopEnd),
            enabled = additionalButtonsEnabled,
            onClick = onEditClick
        )
        CommonButton(
            modifier = Modifier.align(Alignment.TopEnd),
            clicked = additionalButtonsEnabled,
            onClick = onCommonButtonClick
        )
    }
}

@Composable
private fun DeleteButton(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onClick: () -> Unit
) {
    AnimatableButtonBox(
        modifier = modifier,
        enabled = enabled,
        xOffset = -60F,
    ) {
        RoundButton(
            background = MainTheme.colors.deckRepetitionScreen.deleteButton,
            iconId = R.drawable.ic_delete_24,
            onClick = onClick,
            elevation = 4.dp
        )
    }
}

@Composable
private fun AddButton(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onClick: () -> Unit
) {
    AnimatableButtonBox(
        modifier = modifier,
        enabled = enabled,
        yOffset = 60F
    ) {
        RoundButton(
            background = MainTheme.colors.deckRepetitionScreen.addButton,
            iconId = R.drawable.ic_add_24,
            onClick = onClick,
            elevation = 4.dp
        )
    }
}

@Composable
private fun EditButton(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onClick: () -> Unit
) {
    AnimatableButtonBox(
        modifier = modifier,
        enabled = enabled,
        xOffset = -50F,
        yOffset = 50F,
    ) {
        RoundButton(
            background = MainTheme.colors.deckRepetitionScreen.editButton,
            iconId = R.drawable.ic_edit_24,
            enabled = enabled,
            onClick = onClick,
            elevation = 4.dp
        )
    }
}

@Composable
private fun AnimatableButtonBox(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    xOffset: Float = 0F,
    yOffset: Float = 0F,
    content: @Composable BoxScope.() -> Unit,
) {
    val animateState: @Composable (Float) -> State<Float> = { value ->
        animateFloatAsState(
            targetValue = value,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessVeryLow,
            )
        )
    }

    val xTransitionOffset by animateState(if (enabled) xOffset else 0F)
    val yTransitionOffset by animateState(if (enabled) yOffset else 0F)
    val degrees by animateState(if (enabled) 0F else 360F)
    val alpha by animateFloatAsState(
        targetValue = if (enabled) 1F else 0F,
        animationSpec = tween(durationMillis = 500)
    )

    Box(
        modifier = modifier
            .offset(x = xTransitionOffset.dp, y = yTransitionOffset.dp)
            .alpha(alpha)
            .rotate(degrees = degrees),
        content = content,
    )
}

@Composable
private fun CommonButton(
    clicked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale by animateDpAsState(
        targetValue = if (clicked) 40.dp else 50.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy)
    )

    val color by animateColorAsState(
        targetValue = when {
            clicked -> MainTheme.colors.deckRepetitionScreen.mainButtonPressed
            else -> MainTheme.colors.deckRepetitionScreen.mainButtonUnpressed
        },
        animationSpec = tween(durationMillis = 200)
    )

    RoundButton(
        modifier = modifier.size(scale),
        background = color,
        iconId = R.drawable.ic_more_vert_24,
        onClick = onClick,
        elevation = 4.dp
    )
}

@Composable
fun CardButton(
    cardSide: CardSide,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotationValue: Float
    val backgroundColor: Color
    val animationDuration = 100

    when (cardSide) {
        CardSide.FRONT -> {
            rotationValue = 180F
            backgroundColor = MainTheme.colors.deckRepetitionScreen.frontSideCardButton
        }

        CardSide.BACK -> {
            rotationValue = 0F
            backgroundColor = MainTheme.colors.deckRepetitionScreen.backSideCardButton
        }
    }

    val rotation by animateFloatAsState(
        targetValue = rotationValue,
        animationSpec = tween(durationMillis = animationDuration, easing = LinearEasing)
    )

    val color by animateColorAsState(
        targetValue = backgroundColor,
        tween(durationMillis = animationDuration, easing = LinearEasing)
    )

    Card(
        modifier = modifier
            .size(width = 40.dp, height = 56.dp)
            .graphicsLayer { rotationY = rotation },
        shape = RoundedCornerShape(8.dp),
    ) {
        Icon(
            modifier = Modifier
                .background(color)
                .clickable { onClick() }
                .padding(8.dp),
            painter = painterResource(id = R.drawable.ic_rotate_24),
            contentDescription = null,
        )
    }
}

@Composable
private fun ExitDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val coroutineScope = rememberCoroutineScope()

    FullBackgroundDialog(
        onBackgroundClick = { onDismiss() },
        topContent = ContentHolder(size = DIALOG_APP_LABEL_SIZE.dp) { DialogAppLabel() },
        mainContent = {
            Text(
                text = stringResource(
                    id = R.string.deck_repeating_exit_dialog_question
                ),
                textAlign = TextAlign.Center
            )
        },
        bottomContent = {
            RoundButton(
                background = MainTheme.colors.common.positiveDialogButton,
                iconId = R.drawable.ic_confirmation_24,
                onClick = {
                    coroutineScope.launch {
                        onConfirm()
                        awaitFrame()
                        onBackPressedDispatcher?.onBackPressed()
                    }
                }
            )

            RoundButton(
                background = MainTheme.colors.common.neutralDialogButton,
                iconId = R.drawable.ic_close_24,
                onClick = onDismiss
            )
        }
    )
}
