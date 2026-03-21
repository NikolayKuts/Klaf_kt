package com.kuts.klaf.deckRepetition

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.kuts.domain.common.CardRepetitionOrder
import com.kuts.domain.common.CardSide
import com.kuts.domain.common.DeckRepetitionState
import com.kuts.domain.common.ifTrue
import com.kuts.domain.entities.Card
import com.kuts.domain.entities.CefrLevel
import com.kuts.domain.entities.WordMeaningInsights
import com.kuts.domain.entities.WordMeaningItem
import com.kuts.domain.enums.DifficultyRecallingLevel.EASY
import com.kuts.domain.enums.DifficultyRecallingLevel.GOOD
import com.kuts.domain.enums.DifficultyRecallingLevel.HARD
import com.kuts.domain.ipa.IpaHolder
import com.kuts.domain.ipa.LetterInfo
import com.kuts.domain.ipa.toIpaPrompts
import com.kuts.klaf.common.BaseMainViewModel
import com.kuts.klaf.common.ButtonState
import com.kuts.klaf.common.ContentHolder
import com.kuts.klaf.common.DIALOG_APP_LABEL_SIZE
import com.kuts.klaf.common.DialogAppLabel
import com.kuts.klaf.common.FullBackgroundDialog
import com.kuts.klaf.common.Pointer
import com.kuts.klaf.common.RoundButton
import com.kuts.klaf.common.RepetitionTimerState
import com.kuts.klaf.common.ScrollableBox
import com.kuts.klaf.common.TimerCountingState
import com.kuts.klaf.common.WordInsightsBottomSheetContent
import com.kuts.klaf.common.timeAsString
import com.kuts.klaf.deckRepetitionInfo.RepetitionInfoEvent.Non
import com.kuts.klaf.navigation.AppDestination
import com.kuts.klaf.navigation.CollectFlowWithLifecycle
import com.kuts.klaf.navigation.ObserveAudioLifecycle
import com.kuts.klaf.presentation.resources.*
import com.kuts.klaf.theme.MainTheme
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun DeckRepetitionScreen(
    navController: NavHostController,
    backStackEntry: NavBackStackEntry,
    sharedViewModel: BaseMainViewModel,
    deckId: Int,
    deckName: String,
) {
    val viewModel: BaseDeckReviewViewModel = koinViewModel(
        viewModelStoreOwner = backStackEntry,
        parameters = { parametersOf(deckId) },
    )

    ObserveAudioLifecycle(
        onCreate = viewModel.audioPlayer::onCreate,
        onResume = viewModel.audioPlayer::onResume,
        onStop = viewModel.audioPlayer::onStop,
        onDestroy = viewModel.audioPlayer::onDestroy,
    )

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(key1 = lifecycleOwner, key2 = viewModel.timer) {
        lifecycleOwner.lifecycle.addObserver(viewModel.timer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(viewModel.timer)
        }
    }

    CollectFlowWithLifecycle(flow = viewModel.eventMessage, onEach = sharedViewModel::notify)

    CollectFlowWithLifecycle(flow = viewModel.screenState) { state ->
        if (
            state is RepetitionScreenState.FinishState
            && state.repetitionInfoEvent != Non
        ) {
            navController.navigate(
                route = AppDestination.DeckRepetitionInfoDialog(
                    deckId = deckId,
                    deckName = deckName,
                    repetitionInfoEvent = state.repetitionInfoEvent,
                )
            )
        }
    }

    val screenState by viewModel.screenState.collectAsState(RepetitionScreenState.StartState)
    val deckRepetitionState by viewModel.cardState.collectAsState(initial = null)
    val deck by viewModel.deck.collectAsState(initial = null)
    val mainButtonState by viewModel.mainButtonState.collectAsState()
    val deckReviewState by viewModel.deckReviewState.collectAsState()
    val areInsightsAvailable by viewModel.isInsightsAvailable.collectAsState()
    val isInsightsSheetVisible by viewModel.isInsightsSheetVisible.collectAsState()
    val timerState by viewModel.timer.timerState.collectAsState()
    var showExitDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = screenState == RepetitionScreenState.RepetitionState) {
        showExitDialog = showExitDialog.not()
    }

    Surface {
        if (deckRepetitionState != null && deck != null) {
            DeckRepetitionContent(
                deckName = deck!!.name,
                deckRepetitionState = deckRepetitionState!!,
                mainButtonState = mainButtonState,
                screenState = screenState,
                deckReviewState = deckReviewState,
                timerState = timerState,
                areInsightsAvailable = areInsightsAvailable,
                isInsightsSheetVisible = isInsightsSheetVisible,
                showExitDialog = showExitDialog,
                onShowExitDialogChange = { showExitDialog = it },
                onExitConfirmed = { navController.popBackStack() },
                onDeleteCardClick = { cardId ->
                    navController.navigate(
                        route = AppDestination.DeckRepetitionCardDeletingDialog(
                            deckId = deckId,
                            cardId = cardId,
                        )
                    )
                },
                onAddCardClick = {
                    navController.navigate(route = AppDestination.CardAddition(deckId = deckId))
                },
                onEditCardClick = { cardId ->
                    navController.navigate(
                        route = AppDestination.CardEditing(
                            deckId = deckId,
                            cardId = cardId,
                        )
                    )
                },
                onWordClick = viewModel::pronounceWord,
                onShowInsightsClick = viewModel::showInsightsSheet,
                onHideInsightsClick = viewModel::hideInsightsSheet,
                onStartButtonClick = viewModel::startRepeating,
                onEasyButtonClick = { viewModel.moveCardByDifficultyRecallingLevel(level = EASY) },
                onGoodButtonClick = { viewModel.moveCardByDifficultyRecallingLevel(level = GOOD) },
                onHardButtonClick = { viewModel.moveCardByDifficultyRecallingLevel(level = HARD) },
                onCardButtonClick = viewModel::turnCard,
                onSwitchRepetitionOrderClick = viewModel::changeRepetitionOrder,
                onCommonButtonClick = viewModel::changeButtonsStateOnCommonButtonClick,
                onPauseTimer = viewModel::pauseTimerCounting,
                onResumeTimer = viewModel::resumeTimerCounting,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeckRepetitionContent(
    deckName: String,
    deckRepetitionState: DeckRepetitionState,
    mainButtonState: ButtonState,
    screenState: RepetitionScreenState,
    deckReviewState: DeckReviewState,
    timerState: RepetitionTimerState,
    areInsightsAvailable: Boolean,
    isInsightsSheetVisible: Boolean,
    showExitDialog: Boolean,
    onShowExitDialogChange: (Boolean) -> Unit,
    onExitConfirmed: () -> Unit,
    onDeleteCardClick: (cardId: Int) -> Unit,
    onAddCardClick: () -> Unit,
    onEditCardClick: (cardId: Int) -> Unit,
    onWordClick: () -> Unit,
    onShowInsightsClick: () -> Unit,
    onHideInsightsClick: () -> Unit,
    onStartButtonClick: () -> Unit,
    onEasyButtonClick: () -> Unit,
    onGoodButtonClick: () -> Unit,
    onHardButtonClick: () -> Unit,
    onCardButtonClick: () -> Unit,
    onSwitchRepetitionOrderClick: () -> Unit,
    onCommonButtonClick: () -> Unit,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit,
) {
    val density = LocalDensity.current
    val minContentHeightPx = density.run { 400.dp.toPx() }
    val currentCard = deckRepetitionState.card
    val insightsSheetState = rememberModalBottomSheetState()

    ScrollableBox { parentHeightPx ->
        val contentHeight = when {
            parentHeightPx < minContentHeightPx -> minContentHeightPx
            else -> parentHeightPx
        }

        Row {
            Text(
                text = stringResource(
                    resource = Res.string.deck_review_stat_reviewed_cards,
                    deckReviewState.reviewedCardsCount
                )
            )

            Spacer(Modifier.width(10.dp))

            Text(
                text = stringResource(
                    resource = Res.string.deck_review_stat_max_time,
                    deckReviewState.maxTime.timeAsString
                )
            )

            Spacer(Modifier.width(10.dp))

            Text(
                text = stringResource(
                    resource = Res.string.deck_review_stat_left_time,
                    deckReviewState.leftTime.timeAsString
                )
            )
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
                    deckName = deckName,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.size(8.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    OrderPointers(
                        order = deckRepetitionState.repetitionOrder,
                        onSwitchIconClick = onSwitchRepetitionOrderClick,
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
                    Timer(
                        timerState = timerState,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                DeckCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 8.dp),
                    deckRepetitionState = deckRepetitionState,
                    onWordClick = onWordClick,
                )

                InsightsSheetHandle(
                    modifier = Modifier
                        .padding(bottom = 8.dp),
                    isEnabled = areInsightsAvailable,
                    onClick = onShowInsightsClick,
                )

                RepetitionButtons(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    deckRepetitionState = deckRepetitionState,
                    screenState = screenState,
                    onStartButtonClick = onStartButtonClick,
                    onEasyButtonClick = onEasyButtonClick,
                    onGoodButtonClick = onGoodButtonClick,
                    onHardButtonClick = onHardButtonClick,
                    onCardButtonClick = onCardButtonClick,
                )
            }

            AdditionalButtons(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 32.dp),
                additionalButtonsEnabled = mainButtonState == ButtonState.PRESSED,
                onDeleteClick = { deckRepetitionState.card?.id?.let(onDeleteCardClick) },
                onAddClick = onAddCardClick,
                onEditClick = { deckRepetitionState.card?.id?.let(onEditCardClick) },
                onCommonButtonClick = onCommonButtonClick,
            )
        }
    }

    showExitDialog.ifTrue {
        ExitDialog(
            onDismiss = { onShowExitDialogChange(false) },
            onConfirm = {
                onShowExitDialogChange(false)
                onExitConfirmed()
            },
        )
    }

    val shouldPauseTimer = showExitDialog || isInsightsSheetVisible

    LaunchedEffect(key1 = shouldPauseTimer) {
        if (shouldPauseTimer) {
            onPauseTimer()
        } else {
            onResumeTimer()
        }
    }

    if (isInsightsSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = onHideInsightsClick,
            sheetState = insightsSheetState,
        ) {
            WordInsightsBottomSheetContent(
                word = currentCard?.wordMeaningInsights?.word.orEmpty(),
                meanings = currentCard?.wordMeaningInsights?.meanings.orEmpty(),
            )
        }
    }
}

@Composable
private fun InsightsSheetHandle(
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val wordInsightsColors = MainTheme.colors.wordInsightsBottomSheet
    val containerColor = if (isEnabled) {
        wordInsightsColors.reviewHandleEnabledContainer
    } else {
        wordInsightsColors.reviewHandleDisabledContainer
    }
    val textColor = if (isEnabled) {
        wordInsightsColors.reviewHandleEnabledContent
    } else {
        wordInsightsColors.reviewHandleDisabledContent
    }

    Box(
        modifier = modifier
            .clip(shape = RoundedCornerShape(16.dp))
            .background(color = containerColor)
            .let { currentModifier ->
                if (isEnabled) currentModifier.clickable(onClick = onClick) else currentModifier
            }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(resource = Res.string.word_insights_handle_label),
            color = textColor,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun DeckInfo(deckName: String, modifier: Modifier = Modifier) {
    Pointer(
        modifier = modifier,
        pointerTextRes = Res.string.pointer_deck,
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
        CardRepetitionOrder.NATIVE_TO_FOREIGN -> stringResource(resource = Res.string.pointer_native)
        CardRepetitionOrder.FOREIGN_TO_NATIVE -> stringResource(resource = Res.string.pointer_foreign)
    }
    val backSidePointerText = when (order) {
        CardRepetitionOrder.NATIVE_TO_FOREIGN -> stringResource(resource = Res.string.pointer_foreign)
        CardRepetitionOrder.FOREIGN_TO_NATIVE -> stringResource(resource = Res.string.pointer_native)
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
            painter = painterResource(resource = Res.drawable.ic_rotate_24),
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
    timerState: RepetitionTimerState,
    modifier: Modifier = Modifier
) {
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

@Preview(
    name = "Light",
    showBackground = true,
    backgroundColor = 0xFFF4F0E8
)
@Composable
private fun DeckRepetitionContentPreview() {
    DeckRepetitionContentPreviewContent(darkTheme = false)
}

@Preview(name = "Dark", showBackground = true, backgroundColor = 0xFF1B1B1F)
@Composable
private fun DeckRepetitionContentDarkPreview() {
    DeckRepetitionContentPreviewContent(darkTheme = true)
}

@Composable
private fun DeckRepetitionContentPreviewContent(darkTheme: Boolean) {
    MainTheme(darkTheme = darkTheme) {
        DeckRepetitionContent(
            deckName = "French Basics",
            deckRepetitionState = DeckRepetitionState(
                card = Card(
                    deckId = 1,
                    nativeWord = "hello",
                    foreignWord = "bonjour",
                    ipa = listOf(
                        IpaHolder(letterGroup = "bon", ipa = "bɔ̃", groupIndex = 0),
                        IpaHolder(letterGroup = "jour", ipa = "ʒuʁ", groupIndex = 1),
                    ),
                    wordMeaningInsights = WordMeaningInsights(
                        word = "bonjour",
                        language = "fr",
                        meanings = listOf(
                            WordMeaningItem(
                                frequencyRank = 120,
                                translation = "hello",
                                proficiencyLevel = CefrLevel.A1,
                                context = "Common greeting",
                                examples = listOf("Bonjour, Marie."),
                            )
                        ),
                    ),
                    id = 7,
                ),
                side = CardSide.BACK,
                repetitionOrder = CardRepetitionOrder.NATIVE_TO_FOREIGN,
            ),
            mainButtonState = ButtonState.PRESSED,
            screenState = RepetitionScreenState.RepetitionState,
            deckReviewState = DeckReviewState(
                reviewedCardsCount = 12,
                leftTime = 95,
                maxTime = 180,
            ),
            timerState = RepetitionTimerState(
                time = "01:25",
                totalSeconds = 85,
                countingState = TimerCountingState.RUN,
            ),
            areInsightsAvailable = true,
            isInsightsSheetVisible = false,
            showExitDialog = false,
            onShowExitDialogChange = {},
            onExitConfirmed = {},
            onDeleteCardClick = {},
            onAddCardClick = {},
            onEditCardClick = {},
            onWordClick = {},
            onShowInsightsClick = {},
            onHideInsightsClick = {},
            onStartButtonClick = {},
            onEasyButtonClick = {},
            onGoodButtonClick = {},
            onHardButtonClick = {},
            onCardButtonClick = {},
            onSwitchRepetitionOrderClick = {},
            onCommonButtonClick = {},
            onPauseTimer = {},
            onResumeTimer = {},
        )
    }
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
                RepetitionButton(textResId = Res.string.start, onClick = onStartButtonClick)
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
                    RepetitionButton(textResId = Res.string.hard, onClick = onHardButtonClick)
                    RepetitionButton(textResId = Res.string.good, onClick = onGoodButtonClick)
                    RepetitionButton(textResId = Res.string.easy, onClick = onEasyButtonClick)
                }
            }
        }

        is RepetitionScreenState.FinishState -> {}
    }
}

@Composable
private fun RepetitionButton(
    textResId: StringResource,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        modifier = modifier,
        onClick = onClick,
    ) {
        Text(text = stringResource(resource = textResId))
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
            iconRes = Res.drawable.ic_delete_24,
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
            iconRes = Res.drawable.ic_add_24,
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
            iconRes = Res.drawable.ic_edit_24,
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
        iconRes = Res.drawable.ic_more_vert_24,
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

    Box(
        modifier = modifier
            .size(width = 40.dp, height = 56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color)
            .graphicsLayer { rotationY = rotation },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier
                .clickable { onClick() }
                .padding(8.dp),
            painter = painterResource(resource = Res.drawable.ic_rotate_24),
            contentDescription = null,
        )
    }
}

@Composable
private fun ExitDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    FullBackgroundDialog(
        onBackgroundClick = { onDismiss() },
        topContent = ContentHolder(size = DIALOG_APP_LABEL_SIZE.dp) { DialogAppLabel() },
        mainContent = {
            Text(
                text = stringResource(
                    resource = Res.string.deck_repeating_exit_dialog_question
                ),
                textAlign = TextAlign.Center
            )
        },
        bottomContent = {
            RoundButton(
                background = MainTheme.colors.common.positiveDialogButton,
                iconRes = Res.drawable.ic_confirmation_24,
                onClick = onConfirm,
            )

            RoundButton(
                background = MainTheme.colors.common.neutralDialogButton,
                iconRes = Res.drawable.ic_close_24,
                onClick = onDismiss
            )
        }
    )
}
