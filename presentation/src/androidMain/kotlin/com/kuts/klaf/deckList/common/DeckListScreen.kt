package com.kuts.klaf.deckList.common

import android.app.Activity.CLIPBOARD_SERVICE
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.kuts.domain.common.AuthenticationAction
import com.kuts.domain.common.ScheduledDateState
import com.kuts.domain.common.isEven
import com.kuts.domain.entities.Deck
import com.kuts.klaf.authentication.AuthenticationActionResult
import com.kuts.klaf.common.BaseMainViewModel
import com.kuts.klaf.common.ContentHolder
import com.kuts.klaf.common.EventMessage
import com.kuts.klaf.common.FullBackgroundDialog
import com.kuts.klaf.common.NavigationDestination
import com.kuts.klaf.common.ROUNDED_ELEMENT_SIZE
import com.kuts.klaf.common.RoundButton
import com.kuts.klaf.common.RoundedIcon
import com.kuts.klaf.common.SecretConstants
import com.kuts.klaf.common.getScheduledDateStateByByCalculatedRange
import com.kuts.klaf.common.noRippleClickable
import com.kuts.klaf.common.rememberAsMutableStateOf
import com.kuts.klaf.deckList.common.IDeckListNavigationEvent.ToCardTransferringScreen
import com.kuts.klaf.deckList.common.IDeckListNavigationEvent.ToChatGptWithDeckContentPrompt
import com.kuts.klaf.deckList.common.IDeckListNavigationEvent.ToDataSynchronizationDialog
import com.kuts.klaf.deckList.common.IDeckListNavigationEvent.ToDeckCreationDialog
import com.kuts.klaf.deckList.common.IDeckListNavigationEvent.ToDeckNavigationDialog
import com.kuts.klaf.deckList.common.IDeckListNavigationEvent.ToDeckRepetitionScreen
import com.kuts.klaf.deckList.common.IDeckListNavigationEvent.ToDrawerActionDialog
import com.kuts.klaf.deckList.common.IDeckListNavigationEvent.ToPrevious
import com.kuts.klaf.deckList.common.IDeckListNavigationEvent.ToSigningTypeChoosingDialog
import com.kuts.klaf.deckList.drawer.Drawer
import com.kuts.klaf.deckList.drawer.DrawerAction
import com.kuts.klaf.deckList.drawer.DrawerViewState
import com.kuts.klaf.navigation.AUTHENTICATION_RESULT_KEY
import com.kuts.klaf.navigation.AppDestination
import com.kuts.klaf.navigation.CollectFlowWithLifecycle
import com.kuts.klaf.presentation.resources.*
import com.kuts.klaf.theme.MainTheme
import com.lib.lokdroid.core.logE
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun DeckListScreen(
    navController: NavHostController,
    backStackEntry: NavBackStackEntry,
    sharedViewModel: BaseMainViewModel,
    onRestartApp: () -> Unit,
) {
    val context = navController.context
    val chatGptStoryCrafterPromptTemplate = stringResource(
        resource = Res.string.chat_gpt_story_crafter_prompt,
        "%1\$s",
    )

    val viewModel: BaseDeckListViewModel = koinViewModel(viewModelStoreOwner = backStackEntry)

    CollectFlowWithLifecycle(flow = viewModel.eventMessage, onEach = sharedViewModel::notify)

    CollectFlowWithLifecycle(flow = viewModel.navigationEvent) { event ->
        when (event) {
            ToDataSynchronizationDialog -> {
                navController.navigate(route = AppDestination.DataSynchronizationDialog())
            }

            ToDeckCreationDialog -> {
                navController.navigate(route = AppDestination.DeckCreationDialog)
            }

            is ToDeckNavigationDialog -> {
                navController.navigate(
                    route = AppDestination.DeckNavigationDialog(
                        deckId = event.deck.id,
                        deckName = event.deck.name,
                    )
                )
            }

            is ToDeckRepetitionScreen -> {
                navController.navigate(
                    route = AppDestination.DeckRepetition(
                        deckId = event.deck.id,
                        deckName = event.deck.name,
                    )
                )
            }

            ToPrevious -> navController.popBackStack()

            is ToCardTransferringScreen -> {
                navController.navigate(route = AppDestination.CardTransferring(sourceDeckId = event.deckId))
            }

            is ToSigningTypeChoosingDialog -> {
                navController.navigate(
                    route = AppDestination.SigningTypeChoosingDialog(
                        fromSourceDestination = event.fromSourceDestination
                    )
                )
            }

            is ToDrawerActionDialog -> {
                navController.navigate(route = AppDestination.DrawerActionDialog(drawerAction = event.action))
            }

            is ToChatGptWithDeckContentPrompt -> {
                val chatGptStoryCrafterPrompt = String.format(
                    Locale.getDefault(),
                    chatGptStoryCrafterPromptTemplate,
                    event.foreignWords,
                )

                context.copyToClipboard(text = chatGptStoryCrafterPrompt)
                sharedViewModel.notify(message = event.event)
                if (!context.navigateToChatGpt()) {
                    sharedViewModel.notify(
                        message = EventMessage(
                            resId = Res.string.chat_gpt_opening_failed,
                            type = EventMessage.Type.Negative,
                        )
                    )
                }
            }

            null -> {}
        }
    }

    CollectFlowWithLifecycle(
        flow = backStackEntry.savedStateHandle.getStateFlow<String?>(
            key = AUTHENTICATION_RESULT_KEY,
            initialValue = null,
        ),
    ) { rawAuthenticationResult ->
        val authenticationResult = rawAuthenticationResult?.let { serialized ->
            runCatching {
                Json.decodeFromString<AuthenticationActionResult>(serialized)
            }.getOrNull()
        }

        if (authenticationResult == null || authenticationResult.isSuccessful.not()) {
            return@CollectFlowWithLifecycle
        }

        val messageId = when (authenticationResult.action) {
            AuthenticationAction.SIGN_IN -> {
                Res.string.authentication_sign_in_success
            }

            AuthenticationAction.SIGN_UP -> {
                Res.string.authentication_sign_up_success
            }
        }

        sharedViewModel.notify(
            message = EventMessage(
                resId = messageId,
                type = EventMessage.Type.Positive,
            )
        )

        backStackEntry.savedStateHandle[AUTHENTICATION_RESULT_KEY] = null
    }

    Surface {
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        val closeDrawerAndPerform: (performBlock: () -> Unit) -> Unit = { performBlock ->
            scope.launch {
                drawerState.close()
                performBlock.invoke()
            }
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                Drawer(
                    state = viewModel.drawerState.collectAsState(
                        initial = DrawerViewState(
                            signedIn = false,
                            userEmail = null,
                        )
                    ).value,
                    onLogInClick = {
                        closeDrawerAndPerform {
                            viewModel.handleNavigation(
                                event = ToSigningTypeChoosingDialog(
                                    fromSourceDestination = NavigationDestination.DECK_LIST_FRAGMENT
                                )
                            )
                        }
                    },
                    onLogOutClick = {
                        closeDrawerAndPerform {
                            viewModel.handleNavigation(
                                event = ToDrawerActionDialog(action = DrawerAction.LOG_OUT)
                            )
                        }
                    },
                    onDeleteAccountClick = {
                        closeDrawerAndPerform {
                            viewModel.handleNavigation(
                                event = ToDrawerActionDialog(action = DrawerAction.DELETE_ACCOUNT)
                            )
                        }
                    },
                )
            },
        ) {
            DeckListContent(
                decks = viewModel.deckSource.collectAsState().value,
                shouldSynchronizationIndicatorBeShown = viewModel.shouldSynchronizationIndicatorBeShown
                    .collectAsState()
                    .value,
                onItemClick = { deck ->
                    viewModel.handleNavigation(event = ToDeckRepetitionScreen(deck = deck))
                },
                onLongItemClick = { deck ->
                    viewModel.handleNavigation(event = ToDeckNavigationDialog(deck = deck))
                },
                onRefresh = {
                    viewModel.handleNavigation(event = ToDataSynchronizationDialog)
                },
                onMainButtonClick = {
                    viewModel.handleNavigation(event = ToDeckCreationDialog)
                },
                onRestartApp = {
                    viewModel.reopenApp()
                    onRestartApp()
                },
            )
        }
    }
}

private fun Context.copyToClipboard(text: String) {
    val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("prompt", text)

    clipboard.setPrimaryClip(clip)
}

private fun Context.navigateToChatGpt(): Boolean {
    val url = SecretConstants.ChatGpt.STORY_CRAFTER_URL
        .takeUnless { it.isBlank() || it.equals("empty", ignoreCase = true) }
        ?: "https://chatgpt.com/"

    val uri = url.toUri()
    val isSupportedScheme = uri.scheme == "http" || uri.scheme == "https"
    if (!isSupportedScheme) {
        return false
    }

    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    val canHandleIntent = intent.resolveActivity(packageManager) != null
    if (!canHandleIntent) {
        return false
    }

    return runCatching {
        startActivity(intent)
        true
    }.onFailure { error ->
        logE("Failed to open ChatGPT url: $url\n${error.stackTraceToString()}")
    }.getOrDefault(false)
}

@Composable
private fun DeckListContent(
    decks: List<Deck>?,
    shouldSynchronizationIndicatorBeShown: Boolean,
    contentPadding: PaddingValues = PaddingValues(0.dp),
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
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues = contentPadding),
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
            RoundedIcon(background = Color.Transparent, iconRes = Res.drawable.ic_sad_face_24)
        },
        mainContent = {
            Text(
                text = stringResource(resource = Res.string.problem_fetching_decks_view_message),
                textAlign = TextAlign.Center
            )
        },
        bottomContent = {
            RoundButton(
                background = MainTheme.colors.common.neutralDialogButton,
                iconRes = Res.drawable.ic_close_24,
                onClick = { onRestartApp() }
            )
        }
    )
}

@Composable
private fun SynchronizationRefreshingIndicator(
    visible: Boolean,
    offsetY: Dp,
    size: Dp = ROUNDED_ELEMENT_SIZE.dp
) {
    if (visible) {
        Card(
            modifier = Modifier
                .size(size)
                .noRippleClickable { }
                .offset(y = offsetY),
            shape = RoundedCornerShape(size),
        ) {
            Icon(
                modifier = Modifier
                    .size(size)
                    .background(MainTheme.colors.common.dialogBackground)
                    .padding(8.dp),
                painter = painterResource(resource = Res.drawable.ic_sync_24),
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
        iconRes = Res.drawable.ic_add_24,
        onClick = onMainButtonClick,
        modifier = Modifier
            .align(alignment = Alignment.BottomEnd)
            .padding(bottom = 48.dp, end = 48.dp),
        elevation = 4.dp
    )
}

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
        animationSpec = tween(durationMillis = 150 + position * 5),
        label = "DeckItemView",
    )

    LaunchedEffect(key1 = null) { animationFloat = 1F }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(2.dp)
            .animateItem()
            .scale(animationFloatState)
            .alpha(animationFloatState)
            .combinedClickable(
                onClick = { onItemClick(deck) },
                onLongClick = { onLongItemClick(deck) }
            ),
        colors = CardDefaults.cardColors(
            containerColor = getCardBackgroundColorByPosition(position)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
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
    var tick by remember(deck.id) { mutableStateOf(0L) }

    LaunchedEffect(key1 = deck.id) {
        while (true) {
            delay(TimeUnit.MINUTES.toMillis(1))
            tick++
        }
    }

    key(tick) {
        val scheduledDateState: ScheduledDateState = deck.getScheduledDateStateByByCalculatedRange()

        Text(
            text = scheduledDateState.range,
            style = getScheduledDateStyleByScheduledDateState(state = scheduledDateState)
        )
    }
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
                append(deck.reviewCount.toString())
            }
            withStyle(style = MainTheme.typographies.deckItemPointer) {
                append(stringResource(resource = Res.string.repetition_quantity_pointer))
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
                append(stringResource(resource = Res.string.card_quantity_pointer))
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
            colors = CardDefaults.cardColors(
                containerColor = MainTheme.colors.material.onBackground,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
            AnimatedSynchronizationLabel()
        }
    }

    LaunchedEffect(key1 = visible) {
        visibilityState.targetState = visible
    }
}
