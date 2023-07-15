package com.kuts.klaf.presentation.deckRepetition

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.Dimension
import com.kuts.domain.common.CardRepetitionOrder
import com.kuts.domain.common.CardSide
import com.kuts.domain.common.DeckRepetitionState
import com.kuts.domain.enums.DifficultyRecallingLevel.*
import com.kuts.domain.ipa.LetterInfo
import com.kuts.domain.ipa.toIpaPrompts
import com.kuts.klaf.R
import com.kuts.klaf.presentation.common.*
import com.kuts.klaf.presentation.theme.MainTheme

private const val DECK_INFO_ID = "deck_info"
private const val ORDER_POINTERS_ID = "repetition_order"
private const val TIMER_VIEW_ID = "time"
private const val WORD_VIEW_ID = "word_view"
private const val IPA_PROMPTS_VIEW_ID = "ipa_prompts"
private const val START_BUTTON_ID = "start_button"
private const val TURN_CARD_SIDE_BUTTON_ID = "turn_card_side_button"
private const val HARD_BUTTON_ID = "hard_button"
private const val GOOD_BUTTON_ID = "good_button"
private const val EASY_BUTTON_ID = "easy_button"

private const val MAIN_BUTTON_ID = "main_button"
private const val DELETE_BUTTON_ID = "delete_button"
private const val EDIT_BUTTON_ID = "edit_button"
private const val ADD_BUTTON_ID = "add_button"


@Composable
fun DeckRepetitionScreen(
    viewModel: BaseDeckRepetitionViewModel,
    onDeleteCardClick: (cardId: Int) -> Unit,
    onAddCardClick: () -> Unit,
    onEditCardClick: (cardId: Int) -> Unit,
) {
    val deckRepetitionState by viewModel.cardState.collectAsState(initial = null)
    val deck by viewModel.deck.collectAsState(initial = null)
    val mainButtonState by viewModel.mainButtonState.collectAsState()
    val screenState by viewModel.screenState.collectAsState()

    val repetitionState = deckRepetitionState ?: return
    val receivedDeck = deck ?: return

    val density = LocalDensity.current
    val minContentHeightPx = density.run { 400.dp.toPx() }

    ScrollableBox { parentHeightPx ->
        val contentHeight = when {
            parentHeightPx < minContentHeightPx -> minContentHeightPx
            else -> parentHeightPx
        }

        ConstraintLayout(
            constraintSet = getConstraints(),
            modifier = Modifier
                .fillParentMaxWidth()
                .height(density.run { contentHeight.toDp() })
                .padding(16.dp)
        ) {
            Text(text = "", modifier = Modifier)
            DeckInfo(deckName = receivedDeck.name)
            OrderPointers(
                order = repetitionState.repetitionOrder,
                onSwitchIconClick = { viewModel.changeRepetitionOrder() }
            )
            Timer(viewModel = viewModel)
            DeckCard(
                deckRepetitionState = repetitionState,
                onWordClick = { viewModel.pronounceWord() }
            )
            RepetitionButtons(
                deckRepetitionState = repetitionState,
                screenState = screenState,
                onStartButtonClick = { viewModel.startRepeating() },
                onEasyButtonClick = { viewModel.moveCardByDifficultyRecallingLevel(level = EASY) },
                onGoodButtonClick = { viewModel.moveCardByDifficultyRecallingLevel(level = GOOD) },
                onHardButtonClick = { viewModel.moveCardByDifficultyRecallingLevel(level = HARD) },
                onCardButtonClick = { viewModel.turnCard() },
            )
            AdditionalButtons(
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
}

@Composable
private fun getConstraints(): ConstraintSet = ConstraintSet {
    val deckInfoRef = createRefFor(id = DECK_INFO_ID)
    val orderPointers = createRefFor(id = ORDER_POINTERS_ID)
    val timerRef = createRefFor(id = TIMER_VIEW_ID)
    val wordRef = createRefFor(id = WORD_VIEW_ID)
    val ipaPromptsRef = createRefFor(id = IPA_PROMPTS_VIEW_ID)
    val repetitionButtonsGuideline = createGuidelineFromBottom(fraction = 0.15F)
    val startButtonRef = createRefFor(id = START_BUTTON_ID)
    val easyButtonRef = createRefFor(id = EASY_BUTTON_ID)
    val turnCardSideButtonRef = createRefFor(id = TURN_CARD_SIDE_BUTTON_ID)
    val hardButtonRef = createRefFor(id = HARD_BUTTON_ID)
    val goodButtonRef = createRefFor(id = GOOD_BUTTON_ID)
    createHorizontalChain(hardButtonRef, goodButtonRef, easyButtonRef)
    val mainButtonRef = createRefFor(id = MAIN_BUTTON_ID)
    val deleteButtonRef = createRefFor(id = DELETE_BUTTON_ID)
    val addButtonRef = createRefFor(id = ADD_BUTTON_ID)
    val editButtonRef = createRefFor(id = EDIT_BUTTON_ID)

    constrain(ref = deckInfoRef) {
        top.linkTo(parent.top)
        start.linkTo(parent.start)
        end.linkTo(parent.end)
        width = Dimension.fillToConstraints
    }

    constrain(ref = orderPointers) {
        top.linkTo(anchor = deckInfoRef.bottom, margin = 8.dp)
        start.linkTo(anchor = parent.start)
    }

    constrain(ref = timerRef) {
        top.linkTo(anchor = deckInfoRef.bottom, margin = 8.dp)
        start.linkTo(anchor = parent.start)
        end.linkTo(anchor = parent.end)
    }

    constrain(ref = wordRef) {
        top.linkTo(anchor = timerRef.bottom)
        start.linkTo(anchor = parent.start)
        end.linkTo(anchor = parent.end)
        bottom.linkTo(anchor = startButtonRef.top)
    }

    constrain(ref = ipaPromptsRef) {
        top.linkTo(anchor = wordRef.bottom)
        start.linkTo(anchor = wordRef.start)
        end.linkTo(anchor = wordRef.end)
    }

    constrain(ref = startButtonRef) {
        bottom.linkTo(anchor = repetitionButtonsGuideline)
        start.linkTo(anchor = parent.start)
        end.linkTo(anchor = parent.end)
    }

    constrain(ref = turnCardSideButtonRef) {
        bottom.linkTo(anchor = repetitionButtonsGuideline)
        end.linkTo(anchor = easyButtonRef.end)
    }

    constrain(ref = hardButtonRef) {
        top.linkTo(anchor = turnCardSideButtonRef.bottom)
        bottom.linkTo(anchor = parent.bottom)
    }

    constrain(ref = goodButtonRef) {
        top.linkTo(anchor = turnCardSideButtonRef.bottom)
        bottom.linkTo(anchor = parent.bottom)
    }

    constrain(easyButtonRef) {
        top.linkTo(anchor = turnCardSideButtonRef.bottom)
        bottom.linkTo(anchor = parent.bottom)
    }

    constrain(ref = mainButtonRef) {
        top.linkTo(anchor = deckInfoRef.bottom, margin = 32.dp)
        end.linkTo(anchor = parent.end)
    }

    constrain(ref = deleteButtonRef) {
        top.linkTo(anchor = mainButtonRef.top)
        end.linkTo(anchor = mainButtonRef.start, margin = 32.dp)
    }

    constrain(ref = addButtonRef) {
        top.linkTo(anchor = mainButtonRef.bottom, margin = 32.dp)
        end.linkTo(anchor = parent.end)
    }

    constrain(ref = editButtonRef) {
        top.linkTo(anchor = deleteButtonRef.bottom, margin = 8.dp)
        end.linkTo(anchor = addButtonRef.start, margin = 8.dp)
    }
}

@Composable
private fun DeckInfo(deckName: String) {
    Pointer(
        modifier = Modifier.layoutId(DECK_INFO_ID),
        pointerTextId = R.string.pointer_deck,
        valueText = deckName
    )
}

@Composable
private fun OrderPointers(
    order: CardRepetitionOrder,
    onSwitchIconClick: () -> Unit,
) {
    val frontSidePointerText = when (order) {
        CardRepetitionOrder.NATIVE_TO_FOREIGN -> stringResource(id = R.string.pointer_native)
        CardRepetitionOrder.FOREIGN_TO_NATIVE -> stringResource(id = R.string.pointer_foreign)
    }
    val backSidePointerText = when (order) {
        CardRepetitionOrder.NATIVE_TO_FOREIGN -> stringResource(id = R.string.pointer_foreign)
        CardRepetitionOrder.FOREIGN_TO_NATIVE -> stringResource(id = R.string.pointer_native)
    }

    Row(modifier = Modifier.layoutId(ORDER_POINTERS_ID)) {
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
private fun Timer(viewModel: BaseDeckRepetitionViewModel) {
    val timerState by viewModel.timer.timerState.collectAsState()
    val timerColor = when (timerState.countingState) {
        TimerCountingState.RUN -> MainTheme.colors.deckRepetitionScreen.timerActive
        else -> MainTheme.colors.deckRepetitionScreen.timerInactive
    }

    Text(
        modifier = Modifier.layoutId(TIMER_VIEW_ID),
        text = timerState.time,
        color = timerColor,
        style = MainTheme.typographies.timerTextStyle
    )
}

@Composable
private fun DeckCard(deckRepetitionState: DeckRepetitionState, onWordClick: () -> Unit) {
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

    Text(
        modifier = Modifier
            .layoutId(WORD_VIEW_ID)
            .clickable { onWordClick() },
        text = word,
        style = wordTextStyle,
    )

    LazyRow(modifier = Modifier.layoutId(IPA_PROMPTS_VIEW_ID)) {
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

@Composable
private fun RepetitionButtons(
    deckRepetitionState: DeckRepetitionState,
    screenState: RepetitionScreenState,
    onStartButtonClick: () -> Unit,
    onEasyButtonClick: () -> Unit,
    onGoodButtonClick: () -> Unit,
    onHardButtonClick: () -> Unit,
    onCardButtonClick: () -> Unit,
) {
    var isOnFinishCalled by rememberAsMutableStateOf(value = false)

    when (screenState) {
        RepetitionScreenState.StartState -> {
            isOnFinishCalled = false

            RepetitionButton(
                layoutId = START_BUTTON_ID,
                textId = R.string.start,
                onClick = onStartButtonClick
            )
        }

        RepetitionScreenState.RepetitionState -> {
            isOnFinishCalled = false

            CardButton(
                cardSide = deckRepetitionState.side,
                onClick = onCardButtonClick
            )

            RepetitionButton(
                layoutId = HARD_BUTTON_ID,
                textId = R.string.hard,
                onClick = onHardButtonClick
            )

            RepetitionButton(
                layoutId = GOOD_BUTTON_ID,
                textId = R.string.good,
                onClick = onGoodButtonClick
            )

            RepetitionButton(
                layoutId = EASY_BUTTON_ID,
                textId = R.string.easy,
                onClick = onEasyButtonClick
            )
        }

        is RepetitionScreenState.FinishState -> {}
    }
}

@Composable
private fun RepetitionButton(layoutId: String, @StringRes textId: Int, onClick: () -> Unit) {
    Button(
        modifier = Modifier.layoutId(layoutId),
        onClick = onClick
    ) {
        Text(text = stringResource(textId))
    }
}

@Composable
private fun AdditionalButtons(
    additionalButtonsEnabled: Boolean,
    onDeleteClick: () -> Unit,
    onAddClick: () -> Unit,
    onEditClick: () -> Unit,
    onCommonButtonClick: () -> Unit,
) {
    DeleteButton(enabled = additionalButtonsEnabled, onClick = onDeleteClick)
    AddButton(enabled = additionalButtonsEnabled, onClick = onAddClick)
    EditButton(enabled = additionalButtonsEnabled, onClick = onEditClick)
    CommonButton(clicked = additionalButtonsEnabled, onClick = onCommonButtonClick)
}

@Composable
private fun DeleteButton(enabled: Boolean, onClick: () -> Unit) {
    AnimatableButtonBox(
        layoutId = DELETE_BUTTON_ID,
        enabled = enabled,
        xOffset = 60F,
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
private fun AddButton(enabled: Boolean, onClick: () -> Unit) {
    AnimatableButtonBox(
        layoutId = ADD_BUTTON_ID,
        enabled = enabled,
        yOffset = -60F
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
private fun EditButton(enabled: Boolean, onClick: () -> Unit) {
    AnimatableButtonBox(
        layoutId = EDIT_BUTTON_ID,
        enabled = enabled,
        xOffset = 50F,
        yOffset = -50F,
    ) {
        RoundButton(
            background = MainTheme.colors.deckRepetitionScreen.editButton,
            iconId = R.drawable.ic_edit_24,
            onClick = { onClick() },
            elevation = 4.dp
        )
    }
}

@Composable
private fun AnimatableButtonBox(
    layoutId: String,
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

    val xTransitionOffset by animateState(if (enabled) 0F else xOffset)
    val yTransitionOffset by animateState(if (enabled) 0F else yOffset)
    val degrees by animateState(if (enabled) 0F else 360F)
    val alpha by animateFloatAsState(
        targetValue = if (enabled) 1F else 0F,
        animationSpec = tween(durationMillis = 500)
    )

    Box(
        modifier = Modifier
            .layoutId(layoutId)
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
        modifier = Modifier
            .layoutId(MAIN_BUTTON_ID)
            .size(scale),
        background = color,
        iconId = R.drawable.ic_more_vert_24,
        onClick = onClick,
        elevation = 4.dp
    )
}

@Composable
fun CardButton(cardSide: CardSide, onClick: () -> Unit) {
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
        modifier = Modifier
            .size(width = 40.dp, height = 56.dp)
            .layoutId(TURN_CARD_SIDE_BUTTON_ID)
            .graphicsLayer { rotationY = rotation },
        shape = RoundedCornerShape(8.dp),
        elevation = 4.dp,
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