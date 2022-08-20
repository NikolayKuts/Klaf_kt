package com.example.klaf.presentation.deckRepetition

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
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

@Composable
fun DeckRepetitionScreen(
    viewModel: RepetitionViewModel,
    onFinishRepetition: (
        currentDuration: String,
        lastDuration: String,
        scheduledDate: Long,
        previousScheduledDate: Long,
        lastRepetitionIterationDate: String?,
        repetitionQuantity: String,
        lastSuccessMark: String,
    ) -> Unit,
) {
    val deckRepetitionState by viewModel.cardState.collectAsState(initial = null)
    val deck by viewModel.deck.collectAsState(initial = null)
    val timerState by viewModel.timer.timerState.collectAsState()
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
        Time(timerState = timerState)
        DeckCard(deckRepetitionState = repetitionState)
        RepetitionButtons(
            screenState = screenState,
            deckRepetitionState = repetitionState,
            viewModel = viewModel,
            onFinish = onFinishRepetition,
        )
        AdditionalButtons()
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
private fun Time(timerState: RepetitionTimerState) {
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
private fun DeckCard(deckRepetitionState: DeckRepetitionState) {
    val card = deckRepetitionState.card ?: return
    val word: String
    var ipaPrompt = emptyList<LetterInfo>()

    val wordColor: Color = when (deckRepetitionState.side) {
        CardSide.FRONT -> MainTheme.colors.frontSideOrderPointer
        CardSide.BACK -> MainTheme.colors.backSideOrderPointer
    }

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

    Text(
        modifier = Modifier.layoutId(WORD_VIEW_ID),
        text = word,
        style = MainTheme.typographies.cardWordTextStyle,
        color = wordColor
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
    screenState: RepetitionScreenState,
    deckRepetitionState: DeckRepetitionState,
    viewModel: RepetitionViewModel,
    onFinish: (
        currentDuration: String,
        lastDuration: String,
        scheduledDate: Long,
        previousScheduledDate: Long,
        lastRepetitionIterationDate: String?,
        repetitionQuantity: String,
        lastSuccessMark: String,
    ) -> Unit,
) {
    val roundButtonColor = when (deckRepetitionState.side) {
        CardSide.FRONT -> MainTheme.colors.frontSideOrderPointer
        CardSide.BACK -> MainTheme.colors.backSideOrderPointer
    }

    when (screenState) {
        RepetitionScreenState.StartState -> {
            Button(
                modifier = Modifier.layoutId(START_BUTTON_ID),
                onClick = { viewModel.startRepeating() }
            ) {
                Text(text = stringResource(id = R.string.start))
            }
        }
        RepetitionScreenState.RepetitionState -> {
            RoundButton(
                background = roundButtonColor,
                iconId = R.drawable.ic_rotate_24,
                modifier = Modifier.layoutId(TURN_BUTTON_ID),
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
            with(screenState) {
                onFinish(
                    currentDuration,
                    previousDuration,
                    scheduledDate,
                    previousScheduledDate,
                    lastRepetitionIterationDate,
                    repetitionQuantity,
                    lastSuccessMark
                )
            }
        }
    }
}

@Composable
private fun AdditionalButtons() {
    val startAnimation = rememberAsMutableStateOf(value = false)

    DeleteButton(animationState = startAnimation.value)
    AddButton(animationState = startAnimation.value)
    EditButton(animationState = startAnimation.value)
    MainButton(animationState = startAnimation)

}

@Composable
private fun DeleteButton(animationState: Boolean) {
    val transition = updateTransition(targetState = animationState, label = "")

    val xOffset by transition.animateFloat(
        transitionSpec = {
            spring(dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessVeryLow
            )
        },
        label = ""
    ) { if (it) 0F else 60F }

    val alpha by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 500) },
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
    ) { if (it) 0F else 360F }

    RoundButton(
        modifier = Modifier
            .layoutId(DELETE_BUTTON_ID)
            .offset(x = xOffset.dp)
            .alpha(alpha)
            .rotate(degrees = degrees),
        background = Color(0xFF999999),
        iconId = R.drawable.ic_delete_24,
        onClick = { /*TODO*/ }
    )
}

@Composable
private fun AddButton(animationState: Boolean) {
    val transition = updateTransition(targetState = animationState, label = "")

    val yOffset by transition.animateFloat(
        transitionSpec = {
            spring(dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessVeryLow
            )
        },
        label = ""
    ) { if (it) 0F else -60F }

    val alpha by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 500) },
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
    ) { if (it) 0F else 360F }

    RoundButton(
        modifier = Modifier
            .layoutId(ADD_BUTTON_ID)
            .offset(y = yOffset.dp)
            .alpha(alpha)
            .rotate(degrees = degrees),
        background = Color(0xFF999999),
        iconId = R.drawable.ic_add_24,
        onClick = { /*TODO*/ }
    )
}

@Composable
private fun EditButton(animationState: Boolean) {
    val transition = updateTransition(targetState = animationState, label = "")

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
        transitionSpec = { tween(durationMillis = 500) },
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
    ) { if (it) 0F else 360F }

    RoundButton(
        modifier = Modifier
            .layoutId(EDIT_BUTTON_ID)
            .offset(x = xOffset.dp, y = yOffset.dp)
            .alpha(alpha)
            .rotate(degrees = degrees),
        background = Color(0xFF999999),
        iconId = R.drawable.ic_edit_24,
        onClick = { /*TODO*/ }
    )
}

@Composable
private fun MainButton(animationState: MutableState<Boolean>) {
    val transition = updateTransition(targetState = animationState, label = null)
    val scale by transition.animateDp(
        transitionSpec = {
            spring(
                dampingRatio = Spring.DampingRatioHighBouncy
            )
        },
        label = ""
    ) { if (it.value) 40.dp else 50.dp }

    RoundButton(
        modifier = Modifier
            .layoutId(MAIN_BUTTON_ID)
            .size(scale),
        background = Color(0xFF999999),
        iconId = R.drawable.fingerprint_button_icon_24,
        onClick = {
            animationState.value = !animationState.value
        }
    )
}

