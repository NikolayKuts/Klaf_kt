package com.example.presentation.deckRepetition

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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.Dimension
import com.example.klaf.R
import com.example.domain.entities.DeckRepetitionState
import com.example.domain.common.ifFalse
import com.example.domain.enums.DifficultyRecallingLevel.*
import com.example.domain.ipa.LetterInfo
import com.example.domain.ipa.decodeToIpaPrompts
import com.example.klaf.presentation.common.*
import com.example.klaf.presentation.theme.MainTheme

private const val DECK_INFO_VIEW_ID = "deck_info"
private const val FRONT_SIDE_POINTER_ID = "front_side_pointer"
private const val BACK_SIDE_POINTER_ID = "back_side_pointer"
private const val ARROW_POINTER_ID = "arrow_pointer"
private const val SWITCH_ORDER_BUTTON_VIEW_ID = "switch_order_button"
private const val TIMER_VIEW_ID = "time"
private const val WORD_VIEW_ID = "word_view"
private const val IPA_PROMPTS_VIEW_ID = "ipa_prompts"
private const val START_BUTTON_ID = "start_button"
private const val TURN_BUTTON_ID = "turn_button"
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
    onFinishRepetition: () -> Unit,
) {
    val deckRepetitionState by viewModel.cardState.collectAsState(initial = null)
    val deck by viewModel.deck.collectAsState(initial = null)
    val mainButtonState by viewModel.mainButtonState.collectAsState()
    val screenState by viewModel.screenState.collectAsState()

    val repetitionState = deckRepetitionState ?: return
    val receivedDeck = deck ?: return

    ConstraintLayout(
        constraintSet = getConstraints(),
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        DeckInfo(deckName = receivedDeck.name)
        OrderPointers(order = repetitionState.repetitionOrder)
        SwitchOrderButton(onClick = { viewModel.changeRepetitionOrder() })
        Time(viewModel = viewModel)
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
            onFinish = onFinishRepetition,
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
            onMainButtonClick = { viewModel.changeStateOnMainButtonClick() }
        )
    }
}

@Composable
private fun getConstraints(): ConstraintSet {
    return ConstraintSet {
        val deckInfoRef = constrainRefFor(id = DECK_INFO_VIEW_ID) {
            top.linkTo(parent.top)
            start.linkTo(parent.start)
            end.linkTo(parent.end)
            width = Dimension.fillToConstraints
        }

        val frontSidePointerRef = constrainRefFor(id = FRONT_SIDE_POINTER_ID) {
            top.linkTo(deckInfoRef.bottom, margin = 8.dp)
            start.linkTo(parent.start)
        }

        val arrowPointerRef = constrainRefFor(id = ARROW_POINTER_ID) {
            top.linkTo(frontSidePointerRef.top)
            start.linkTo(frontSidePointerRef.end)
            bottom.linkTo(frontSidePointerRef.bottom)
        }

        val backSidePointerRef = constrainRefFor(id = BACK_SIDE_POINTER_ID) {
            top.linkTo(frontSidePointerRef.top)
            start.linkTo(arrowPointerRef.end)
        }

        val rotateButtonRef = constrainRefFor(id = SWITCH_ORDER_BUTTON_VIEW_ID) {
            top.linkTo(frontSidePointerRef.bottom)
            start.linkTo(arrowPointerRef.start)
            end.linkTo(arrowPointerRef.end)
        }

        val timerRef = constrainRefFor(id = TIMER_VIEW_ID) {
            top.linkTo(deckInfoRef.bottom, margin = 8.dp)
            start.linkTo(parent.start)
            end.linkTo(parent.end)
        }

        val wordGuideline = createGuidelineFromTop(fraction = 0.4F)

        val wordRef = constrainRefFor(id = WORD_VIEW_ID) {
            top.linkTo(wordGuideline)
            start.linkTo(parent.start)
            end.linkTo(parent.end)
        }

        val ipaPromptsRef = constrainRefFor(id = IPA_PROMPTS_VIEW_ID) {
            top.linkTo(wordRef.bottom)
            start.linkTo(wordRef.start)
            end.linkTo(wordRef.end)
        }

        val buttonsGuideline = createGuidelineFromBottom(fraction = 0.2F)

        val startButtonRef = constrainRefFor(id = START_BUTTON_ID) {
            top.linkTo(buttonsGuideline)
            start.linkTo(parent.start)
            end.linkTo(parent.end)
        }

        val easyButtonRef = createRefFor(id = EASY_BUTTON_ID)

        val turnButtonRef = constrainRefFor(id = TURN_BUTTON_ID) {
            top.linkTo(buttonsGuideline)
            end.linkTo(easyButtonRef.end)
        }

        val hardButtonRef = constrainRefFor(id = HARD_BUTTON_ID) {
            top.linkTo(turnButtonRef.bottom)
            bottom.linkTo(parent.bottom)
        }

        val goodButtonRef = constrainRefFor(id = GOOD_BUTTON_ID) {
            top.linkTo(turnButtonRef.bottom)
            bottom.linkTo(parent.bottom)
        }

        constrain(easyButtonRef) {
            top.linkTo(turnButtonRef.bottom)
            bottom.linkTo(parent.bottom)
        }

        createHorizontalChain(hardButtonRef, goodButtonRef, easyButtonRef)

        val mainButtonRef = constrainRefFor(id = MAIN_BUTTON_ID) {
            top.linkTo(deckInfoRef.bottom, margin = 32.dp)
            end.linkTo(parent.end)
        }

        val deleteButtonRef = constrainRefFor(id = DELETE_BUTTON_ID) {
            top.linkTo(mainButtonRef.top)
            end.linkTo(mainButtonRef.start, margin = 32.dp)
        }

        val addButtonRef = constrainRefFor(id = ADD_BUTTON_ID) {
            top.linkTo(mainButtonRef.bottom, margin = 32.dp)
            end.linkTo(parent.end)
        }

        val editButtonRef = constrainRefFor(id = EDIT_BUTTON_ID) {
            top.linkTo(deleteButtonRef.bottom, margin = 8.dp)
            end.linkTo(addButtonRef.start, margin = 8.dp)
        }

    }
}

@Composable
private fun DeckInfo(deckName: String) {
    Pointer(
        modifier = Modifier.layoutId(DECK_INFO_VIEW_ID),
        pointerTextId = R.string.pointer_deck,
        valueText = deckName
    )
}

@Composable
private fun OrderPointers(
    order: com.example.domain.common.CardRepetitionOrder,
) {
    val frontSidePointerText = when (order) {
        com.example.domain.common.CardRepetitionOrder.NATIVE_TO_FOREIGN -> stringResource(id = R.string.pointer_native)
        com.example.domain.common.CardRepetitionOrder.FOREIGN_TO_NATIVE -> stringResource(id = R.string.pointer_foreign)
    }
    val backSidePointerText = when (order) {
        com.example.domain.common.CardRepetitionOrder.NATIVE_TO_FOREIGN -> stringResource(id = R.string.pointer_foreign)
        com.example.domain.common.CardRepetitionOrder.FOREIGN_TO_NATIVE -> stringResource(id = R.string.pointer_native)
    }

    Text(
        modifier = Modifier.layoutId(FRONT_SIDE_POINTER_ID),
        text = frontSidePointerText,
        style = MainTheme.typographies.frontSideOrderPointer
    )
    Icon(
        modifier = Modifier.layoutId(ARROW_POINTER_ID),
        painter = painterResource(id = R.drawable.ic_order_arow_24),
        contentDescription = null
    )
    Text(
        modifier = Modifier.layoutId(BACK_SIDE_POINTER_ID),
        text = backSidePointerText,
        style = MainTheme.typographies.backSideOrderPointer
    )
}

@Composable
private fun SwitchOrderButton(onClick: () -> Unit) {
    Icon(
        modifier = Modifier
            .layoutId(SWITCH_ORDER_BUTTON_VIEW_ID)
            .clickable { onClick() },
        painter = painterResource(id = R.drawable.ic_rotate_24),
        contentDescription = null
    )
}

@Composable
private fun Time(viewModel: BaseDeckRepetitionViewModel) {
    val timerState by viewModel.timer.timerState.collectAsState()
    val timerColor = when (timerState.countingState) {
        TimerCountingState.RUN -> MainTheme.colors.deckRepetitionScreenColors.timerActive
        else -> MainTheme.colors.deckRepetitionScreenColors.timerInactive
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
        com.example.domain.common.CardRepetitionOrder.NATIVE_TO_FOREIGN -> {
            when (deckRepetitionState.side) {
                com.example.domain.common.CardSide.FRONT -> {
                    word = card.nativeWord
                    ipaPrompt = emptyList()
                }
                com.example.domain.common.CardSide.BACK -> {
                    word = card.foreignWord
                    ipaPrompt = card.decodeToIpaPrompts()
                }
            }
        }
        com.example.domain.common.CardRepetitionOrder.FOREIGN_TO_NATIVE -> {
            when (deckRepetitionState.side) {
                com.example.domain.common.CardSide.FRONT -> {
                    word = card.foreignWord
                    ipaPrompt = card.decodeToIpaPrompts()
                }
                com.example.domain.common.CardSide.BACK -> {
                    word = card.nativeWord
                    ipaPrompt = emptyList()
                }
            }
        }
    }

    val wordTextStyle = when (deckRepetitionState.side) {
        com.example.domain.common.CardSide.FRONT -> MainTheme.typographies.frontSideCardWordTextStyle
        com.example.domain.common.CardSide.BACK -> MainTheme.typographies.backSideCardWordTextStyle
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
                letterInfo.isChecked -> MainTheme.colors.deckRepetitionScreenColors.ipaPromptChecked
                else -> MainTheme.colors.deckRepetitionScreenColors.ipaPromptUnchecked
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
    onFinish: () -> Unit,
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

        is RepetitionScreenState.FinishState -> {
            isOnFinishCalled.ifFalse {
                isOnFinishCalled = true
                onFinish()
            }
        }
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
    onMainButtonClick: () -> Unit,
) {
    DeleteButton(enabled = additionalButtonsEnabled, onClick = onDeleteClick)
    AddButton(enabled = additionalButtonsEnabled, onClick = onAddClick)
    EditButton(enabled = additionalButtonsEnabled, onClick = onEditClick)
    MoreButton(clicked = additionalButtonsEnabled, onClick = onMainButtonClick)
}

@Composable
private fun DeleteButton(enabled: Boolean, onClick: () -> Unit) {
    AnimatableButtonBox(
        layoutId = DELETE_BUTTON_ID,
        enabled = enabled,
        xOffset = 60F,
    ) {
        RoundButton(
            background = MainTheme.colors.deckRepetitionScreenColors.deleteButton,
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
            background = MainTheme.colors.deckRepetitionScreenColors.addButton,
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
            background = MainTheme.colors.deckRepetitionScreenColors.editButton,
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
    content: @Composable BoxScope.() -> Unit
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
private fun MoreButton(
    clicked: Boolean,
    onClick: () -> Unit
) {
    val scale by animateDpAsState(
        targetValue = if (clicked) 40.dp else 50.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy)
    )

    val color by animateColorAsState(
        targetValue = when {
            clicked -> MainTheme.colors.deckRepetitionScreenColors.mainButtonPressed
            else -> MainTheme.colors.deckRepetitionScreenColors.mainButtonUnpressed
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
fun CardButton(cardSide: com.example.domain.common.CardSide, onClick: () -> Unit) {
    val rotationValue: Float
    val backgroundColor: Color
    val animationDuration = 500

    when (cardSide) {
        com.example.domain.common.CardSide.FRONT -> {
            rotationValue = 180F
            backgroundColor = MainTheme.colors.deckRepetitionScreenColors.frontSideCardButton
        }
        com.example.domain.common.CardSide.BACK -> {
            rotationValue = 0F
            backgroundColor = MainTheme.colors.deckRepetitionScreenColors.backSideCardButton
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
            .layoutId(TURN_BUTTON_ID)
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

