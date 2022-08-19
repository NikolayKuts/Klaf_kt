package com.example.klaf.presentation.deckRepetition

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.*
import com.example.klaf.R
import com.example.klaf.domain.common.CardRepetitionOrder
import com.example.klaf.domain.common.CardRepetitionState
import com.example.klaf.domain.common.CardSide
import com.example.klaf.domain.enums.DifficultyRecallingLevel
import com.example.klaf.domain.ipa.LetterInfo
import com.example.klaf.domain.ipa.decodeToIpaPrompts
import com.example.klaf.presentation.common.RoundButton
import com.example.klaf.presentation.common.log
import com.example.klaf.presentation.theme.MainTheme

private const val DECK_INFO_VIEW_ID = "deck_info"
private const val REPETITION_ORDER_POINTERS_VIEW_ID = "order_pointer"
private const val SWITCH_ORDER_BUTTON_VIEW_ID = "switch_order_button"
private const val TIMER_VIEW_ID = "time"
private const val CARD_VIEW_ID = "card"
private const val START_BUTTON_ID = "start_button"
private const val TURN_BUTTON_ID = "turn_button"
private const val HARD_BUTTON_ID = "hard_button"
private const val GOOD_BUTTON_ID = "good_button"
private const val EASY_BUTTON_ID = "easy_button"

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
    val time by viewModel.timer.timerState.collectAsState()
    val screenState by viewModel.screenState.collectAsState()

    deckRepetitionState?.let { repetitionState ->
        log(repetitionState.card.toString())

        deck?.let { receivedDeck ->
//            if (repetitionState == null) return
            log(message = "after null")

            val constraints = ConstraintSet {
                val deckInfoRef = constrainRefFor(id = DECK_INFO_VIEW_ID) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                }

                val repetitionOrderRef = constrainRefFor(id = REPETITION_ORDER_POINTERS_VIEW_ID) {
                    top.linkTo(deckInfoRef.bottom)
                    start.linkTo(parent.start)
                }

                val rotateButtonRef = constrainRefFor(id = SWITCH_ORDER_BUTTON_VIEW_ID) {
                    top.linkTo(repetitionOrderRef.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(repetitionOrderRef.end)
                }

                val timerRef = constrainRefFor(id = TIMER_VIEW_ID) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    top.linkTo(deckInfoRef.bottom)
                }

                val wordGuideline = createGuidelineFromTop(fraction = 0.4F)

                val cardRef = constrainRefFor(id = CARD_VIEW_ID) {
                    top.linkTo(wordGuideline)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
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
//                    start.linkTo(easyButtonRef.start)
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
            }

            ConstraintLayout(
                constraintSet = constraints,
                modifier = Modifier
                    .fillMaxSize()
//                    .background(Color(0xFFCA8975))
                    .padding(16.dp)
            ) {
                DeckInfo(deckName = receivedDeck.name)
                OrderPointers(order = repetitionState.repetitionOrder)
                SwitchOrderButton(
                    onClick = { viewModel.changeRepetitionOrder() }
                )
                Time(time = time.time)
                DeckCard(cardRepetitionState = repetitionState)
                ControlButtons(
                    screenState = screenState,
                    viewModel = viewModel,
                    onFinish = onFinishRepetition
                )
            }
        }
    }
}

private fun ConstraintSetScope.constrainRefFor(
    id: String,
    constrainBlock: ConstrainScope.() -> Unit,
): ConstrainedLayoutReference {
    val reference = createRefFor(id = id)
    constrain(ref = reference, constrainBlock = constrainBlock)

    return reference
}

@Composable
private fun DeckInfo(deckName: String) {
    Row(
        modifier = Modifier
            .background(Color(0xFF96BB6B))
            .layoutId(DECK_INFO_VIEW_ID)
    ) {
        Text(
            modifier = Modifier.background(Color(0xFF8D6DC5)),
            text = "deck"
        )
        Text(
            modifier = Modifier.background(Color(0xFF96BB6B)),
            text = deckName
        )
    }
}

@Composable
private fun OrderPointers(
    order: CardRepetitionOrder,
) {
    Row(
        modifier = Modifier
            .background(Color(0xFFE69B96))
            .layoutId(REPETITION_ORDER_POINTERS_VIEW_ID)
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
            text = frontSidePointerText,
            style = MainTheme.typographies.frontSideOrderPointer
        )
        Icon(painter = painterResource(id = R.drawable.ic_order_arow_24), contentDescription = null)
        Text(
            text = backSidePointerText,
            style = MainTheme.typographies.backSideOrderPointer
        )
    }
}

@Composable
private fun SwitchOrderButton(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .layoutId(SWITCH_ORDER_BUTTON_VIEW_ID)
            .clickable { onClick() }
    ) {
        Icon(painter = painterResource(id = R.drawable.ic_rotate_24), contentDescription = "")
    }
}

@Composable
private fun Time(time: String) {
    Text(
        modifier = Modifier.layoutId(TIMER_VIEW_ID),
        text = time
    )
}

@Composable
private fun DeckCard(cardRepetitionState: CardRepetitionState) {
    val card = cardRepetitionState.card ?: return
    var word = ""
    var ipaPrompt = emptyList<LetterInfo>()

    when (cardRepetitionState.repetitionOrder) {
        CardRepetitionOrder.NATIVE_TO_FOREIGN -> {
            when (cardRepetitionState.side) {
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
            when (cardRepetitionState.side) {
                CardSide.FRONT -> {
                    word = card.foreignWord
                    ipaPrompt = card.decodeToIpaPrompts()
                }
                CardSide.BACK -> {
                    word = card.foreignWord
                    ipaPrompt = emptyList()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .layoutId(CARD_VIEW_ID)
            .background(Color(0xFF5A5A5A))
    ) {
        Text(text = word)
        LazyRow() {
            itemsIndexed(items = ipaPrompt) { _, letterInfo ->
                val promptColor = if (letterInfo.isChecked) {
                    Color(0xFFD87D7D)
                } else {
                    Color(0xFF585858)
                }

                Text(text = letterInfo.letter, color = promptColor)
            }
        }
    }
}

@Composable
private fun ControlButtons(
    screenState: RepetitionScreenState,
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
    when (screenState) {
        RepetitionScreenState.StartState -> {
//            binding.startRepetitionButton.isVisible = true
//            cardRepetitionControlButtons.onEach { it.isVisible = false }
            Button(
                modifier = Modifier.layoutId(START_BUTTON_ID),
                onClick = { viewModel.startRepeating() }
            ) {
                Text(text = "Start")
            }
        }
        RepetitionScreenState.RepetitionState -> {
//            binding.startRepetitionButton.isVisible = false
//            cardRepetitionControlButtons.onEach { it.isVisible = true }

//            Button(
//                modifier = Modifier.layoutId(START_BUTTON_ID),
//                onClick = { viewModel.turnCard() }
//            ) {
//                Text(text = "turn")
//            }

            RoundButton(
                background = MainTheme.colors.materialColors.primary,
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
                Text(text = "hard")
            }
            Button(
                modifier = Modifier.layoutId(GOOD_BUTTON_ID),
                onClick = {
                    viewModel.moveCardByDifficultyRecallingLevel(
                        level = DifficultyRecallingLevel.GOOD
                    )
                }
            ) {
                Text(text = "good")
            }
            Button(
                modifier = Modifier.layoutId(EASY_BUTTON_ID),
                onClick = {
                    viewModel.moveCardByDifficultyRecallingLevel(
                        level = DifficultyRecallingLevel.EASY
                    )
                }
            ) {
                Text(text = "easy")
            }
        }
        is RepetitionScreenState.FinishState -> {
//            binding.startRepetitionButton.isVisible = true
//            cardRepetitionControlButtons.onEach { it.isVisible = false }

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
