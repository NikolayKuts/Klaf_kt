package com.example.klaf.presentation.deckRepetition

import androidx.compose.animation.animateColor
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.example.klaf.domain.common.CardRepetitionOrder
import com.example.klaf.domain.common.CardSide
import com.example.klaf.domain.common.DeckRepetitionState
import com.example.klaf.domain.common.ifFalse
import com.example.klaf.domain.enums.DifficultyRecallingLevel
import com.example.klaf.domain.ipa.LetterInfo
import com.example.klaf.domain.ipa.decodeToIpaPrompts
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

private const val OFFSET_VALUE = 60F
private const val ANIMATION_DURATION = 500
private const val ROTATION_DEGREES = 360F

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
    val additionalButtonsEnabled = rememberAsMutableStateOf(value = false)

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
            viewModel = viewModel,
            onFinish = onFinishRepetition,
        )
        AdditionalButtons(
            additionalButtonsEnabledState = additionalButtonsEnabled,
            onDeleteClick = {
                repetitionState.card?.id?.let { cardId -> onDeleteCardClick(cardId) }
            },
            onAddClick = onAddCardClick,
            onEditClick = {
                repetitionState.card?.id?.let { cardId -> onEditCardClick(cardId) }
            },
            onMainButtonClick = {
                if (additionalButtonsEnabled.value) {
                    viewModel.pauseTimerCounting()
                } else {
                    viewModel.resumeTimerCounting()
                }
            }
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
    order: CardRepetitionOrder,
) {
    val frontSidePointerText = when (order) {
        CardRepetitionOrder.NATIVE_TO_FOREIGN -> stringResource(id = R.string.pointer_native)
        CardRepetitionOrder.FOREIGN_TO_NATIVE -> stringResource(id = R.string.pointer_foreign)
    }
    val backSidePointerText = when (order) {
        CardRepetitionOrder.NATIVE_TO_FOREIGN -> stringResource(id = R.string.pointer_foreign)
        CardRepetitionOrder.FOREIGN_TO_NATIVE -> stringResource(id = R.string.pointer_native)
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
        TimerCountingState.RUN -> MainTheme.colors.timerActive
        else -> MainTheme.colors.timerInactive
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
                    ipaPrompt = card.decodeToIpaPrompts()
                }
            }
        }
        CardRepetitionOrder.FOREIGN_TO_NATIVE -> {
            when (deckRepetitionState.side) {
                CardSide.FRONT -> {
                    word = card.foreignWord
                    ipaPrompt = card.decodeToIpaPrompts()
                }
                CardSide.BACK -> {
                    word = card.nativeWord
                    ipaPrompt = emptyList()
                }
            }
        }
    }

    val wordTextSyle = when (deckRepetitionState.side) {
        CardSide.FRONT -> MainTheme.typographies.frontSideCardWordTextStyle
        CardSide.BACK -> MainTheme.typographies.backSideCardWordTextStyle
    }

    Text(
        modifier = Modifier
            .layoutId(WORD_VIEW_ID)
            .clickable { onWordClick() },
        text = word,
        style = wordTextSyle,
    )

    LazyRow(modifier = Modifier.layoutId(IPA_PROMPTS_VIEW_ID)) {
        itemsIndexed(items = ipaPrompt) { _, letterInfo ->
            val promptColor = if (letterInfo.isChecked) {
                MainTheme.colors.ipaPromptChecked
            } else {
                MainTheme.colors.ipaPromptUnchecked
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
    viewModel: BaseDeckRepetitionViewModel,
    onFinish: () -> Unit,
) {
    val screenStateState = viewModel.screenState.collectAsState()
    var isOnFinishCalled by rememberAsMutableStateOf(value = false)

    when (val screenState = screenStateState.value) {
        RepetitionScreenState.StartState -> {
            isOnFinishCalled = false

            Button(
                modifier = Modifier.layoutId(START_BUTTON_ID),
                onClick = { viewModel.startRepeating() }
            ) {
                Text(text = stringResource(id = R.string.start))
            }
        }
        RepetitionScreenState.RepetitionState -> {
            isOnFinishCalled = false

            CardButton(
                cardSide = deckRepetitionState.side,
                onClick = { viewModel.turnCard() }
            )

            Button(
                modifier = Modifier.layoutId(HARD_BUTTON_ID),
                onClick = {
                    viewModel.moveCardByDifficultyRecallingLevel(
                        level = DifficultyRecallingLevel.HARD
                    )
                }
            ) {
                Text(text = stringResource(R.string.hard))
            }
            Button(
                modifier = Modifier.layoutId(GOOD_BUTTON_ID),
                onClick = {
                    viewModel.moveCardByDifficultyRecallingLevel(
                        level = DifficultyRecallingLevel.GOOD
                    )
                }
            ) {
                Text(text = stringResource(R.string.good))
            }
            Button(
                modifier = Modifier.layoutId(EASY_BUTTON_ID),
                onClick = {
                    viewModel.moveCardByDifficultyRecallingLevel(
                        level = DifficultyRecallingLevel.EASY
                    )
                }
            ) {
                Text(text = stringResource(R.string.easy))
            }
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
private fun AdditionalButtons(
    additionalButtonsEnabledState: MutableState<Boolean>,
    onDeleteClick: () -> Unit,
    onAddClick: () -> Unit,
    onEditClick: () -> Unit,
    onMainButtonClick: () -> Unit,
) {
    DeleteButton(enabled = additionalButtonsEnabledState.value, onClick = onDeleteClick)
    AddButton(enabled = additionalButtonsEnabledState.value, onClick = onAddClick)
    EditButton(enabled = additionalButtonsEnabledState.value, onClick = onEditClick)
    MainButton(animationStateState = additionalButtonsEnabledState, onClick = onMainButtonClick)
}

@Composable
private fun DeleteButton(enabled: Boolean, onClick: () -> Unit) {
    val transition = updateTransition(targetState = enabled, label = null)

    val xOffset by transition.animateFloat(
        transitionSpec = {
            spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessVeryLow)
        },
        label = ""
    ) { if (it) 0F else OFFSET_VALUE }

    val alpha by transition.animateFloat(
        transitionSpec = { tween(durationMillis = ANIMATION_DURATION) },
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
    ) { if (it) 0F else ROTATION_DEGREES }

    RoundButton(
        modifier = Modifier
            .layoutId(DELETE_BUTTON_ID)
            .offset(x = xOffset.dp)
            .alpha(alpha)
            .rotate(degrees = degrees),
        background = MainTheme.colors.deckRepetitionDeleteButton,
        iconId = R.drawable.ic_delete_24,
        onClick = onClick,
        elevation = 4.dp
    )
}

@Composable
private fun AddButton(enabled: Boolean, onClick: () -> Unit) {
    val transition = updateTransition(targetState = enabled, label = "")

    val yOffset by transition.animateFloat(
        transitionSpec = {
            spring(dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessVeryLow
            )
        },
        label = ""
    ) { if (it) 0F else -OFFSET_VALUE }

    val alpha by transition.animateFloat(
        transitionSpec = { tween(durationMillis = ANIMATION_DURATION) },
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
    ) { if (it) 0F else ROTATION_DEGREES }

    RoundButton(
        modifier = Modifier
            .layoutId(ADD_BUTTON_ID)
            .offset(y = yOffset.dp)
            .alpha(alpha)
            .rotate(degrees = degrees),
        background = MainTheme.colors.deckRepetitionAddButton,
        iconId = R.drawable.ic_add_24,
        onClick = onClick,
        elevation = 4.dp
    )
}

@Composable
private fun EditButton(enabled: Boolean, onClick: () -> Unit) {
    val transition = updateTransition(targetState = enabled, label = "")

    val xOffset by transition.animateFloat(
        transitionSpec = {
            spring(dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessVeryLow
            )
        },
        label = ""
    ) { if (it) 0F else 50F }

    val yOffset by transition.animateFloat(
        transitionSpec = {
            spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessVeryLow
            )
        },
        label = ""
    ) { if (it) 0F else -50F }

    val alpha by transition.animateFloat(
        transitionSpec = { tween(durationMillis = ANIMATION_DURATION) },
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
    ) { if (it) 0F else ROTATION_DEGREES }

    RoundButton(
        modifier = Modifier
            .layoutId(EDIT_BUTTON_ID)
            .offset(x = xOffset.dp, y = yOffset.dp)
            .alpha(alpha)
            .rotate(degrees = degrees),
        background = MainTheme.colors.deckRepetitionEditButton,
        iconId = R.drawable.ic_edit_24,
        onClick = { onClick() },
        elevation = 4.dp
    )
}

@Composable
private fun MainButton(animationStateState: MutableState<Boolean>, onClick: () -> Unit) {
    val transition = updateTransition(targetState = animationStateState, label = null)
    val scale by transition.animateDp(
        transitionSpec = {
            spring(dampingRatio = Spring.DampingRatioHighBouncy)
        },
        label = ""
    ) { if (it.value) 40.dp else 50.dp }
    val color by transition.animateColor(
        transitionSpec = { tween(durationMillis = 200) }, label = ""
    ) {
        if (it.value) {
            MainTheme.colors.deckRepetitionMainButtonPressed
        } else {
            MainTheme.colors.deckRepetitionMainButtonUnpressed
        }
    }

    RoundButton(
        modifier = Modifier
            .layoutId(MAIN_BUTTON_ID)
            .size(scale),
        background = color,
        iconId = R.drawable.fingerprint_button_icon_24,
        onClick = {
            animationStateState.value = !animationStateState.value
            onClick()
        },
        elevation = 4.dp
    )
}

@Composable
fun CardButton(cardSide: CardSide, onClick: () -> Unit) {
    val rotationValue: Float
    val backgroundColor: Color
    val animationDuration = 500

    when (cardSide) {
        CardSide.FRONT -> {
            rotationValue = 180F
            backgroundColor = MainTheme.colors.frontSideOrderPointer
        }
        CardSide.BACK -> {
            rotationValue = 0F
            backgroundColor = MainTheme.colors.backSideOrderPointer
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

