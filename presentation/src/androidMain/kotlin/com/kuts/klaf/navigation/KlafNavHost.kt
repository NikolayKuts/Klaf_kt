package com.kuts.klaf.navigation

import android.app.Activity
import android.app.Activity.CLIPBOARD_SERVICE
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.kuts.domain.common.AuthenticationAction
import com.kuts.domain.common.LoadingState
import com.kuts.domain.common.ifTrue
import com.kuts.domain.entities.Deck
import com.kuts.klaf.authentication.AuthenticationActionResult
import com.kuts.klaf.authentication.AuthenticationScreen
import com.kuts.klaf.authentication.BaseAuthenticationViewModel
import com.kuts.klaf.cardManagement.cardAddition.CardAdditionViewModel
import com.kuts.klaf.cardManagement.cardAddition.CardManagementScreen
import com.kuts.klaf.cardManagement.cardEditing.CardEditingViewModel
import com.kuts.klaf.cardManagement.common.CardManagementState
import com.kuts.klaf.cardTransferring.common.BaseCardTransferringViewModel
import com.kuts.klaf.cardTransferring.common.CardTransferringScreen
import com.kuts.klaf.cardTransferring.common.ICardTransferringAction
import com.kuts.klaf.cardTransferring.common.ICardTransferringNavigationDestination.CardTransferringScreen as CardTransferringScreenDestination
import com.kuts.klaf.cardTransferring.common.ICardTransferringNavigationEvent
import com.kuts.klaf.cardTransferring.deckChoosing.DeckChoosingDialogView
import com.kuts.klaf.cardViewing.CardViewingScreen
import com.kuts.klaf.cardViewing.CardViewingViewModel
import com.kuts.klaf.common.BaseMainViewModel
import com.kuts.klaf.common.CardDeletingDialogView
import com.kuts.klaf.common.EventMessage
import com.kuts.klaf.common.NavigationDestination
import com.kuts.klaf.common.SecretConstants
import com.kuts.klaf.deckList.common.BaseDeckListViewModel
import com.kuts.klaf.deckList.common.DeckListScreen
import com.kuts.klaf.deckList.common.IDeckListNavigationEvent.ToCardTransferringScreen
import com.kuts.klaf.deckList.common.IDeckListNavigationEvent.ToChatGptWithDeckContentPrompt
import com.kuts.klaf.deckList.common.IDeckListNavigationEvent.ToDataSynchronizationDialog
import com.kuts.klaf.deckList.common.IDeckListNavigationEvent.ToDeckCreationDialog
import com.kuts.klaf.deckList.common.IDeckListNavigationEvent.ToDeckNavigationDialog
import com.kuts.klaf.deckList.common.IDeckListNavigationEvent.ToDeckRepetitionScreen
import com.kuts.klaf.deckList.common.IDeckListNavigationEvent.ToDrawerActionDialog
import com.kuts.klaf.deckList.common.IDeckListNavigationEvent.ToPrevious
import com.kuts.klaf.deckList.common.IDeckListNavigationEvent.ToSigningTypeChoosingDialog
import com.kuts.klaf.deckList.dataSynchronization.DataSynchronizationDialogView
import com.kuts.klaf.deckList.deckCreation.DeckCreationDialog
import com.kuts.klaf.deckList.deckDeleting.DeckDeletionDialogView
import com.kuts.klaf.deckList.deckNavigation.DeckNavigationDialogView
import com.kuts.klaf.deckList.deckRenaming.DeckRenamingDialog
import com.kuts.klaf.deckList.drawer.Drawer
import com.kuts.klaf.deckList.drawer.DrawerAction
import com.kuts.klaf.deckList.drawer.DrawerActionView
import com.kuts.klaf.deckList.drawer.DrawerViewState
import com.kuts.klaf.deckList.sygningTypeChoosing.SigningTypeChoosingView
import com.kuts.klaf.deckManagment.BaseDeckManagementViewModel
import com.kuts.klaf.deckManagment.DeckManagementScreen
import com.kuts.klaf.deckRepetition.BaseDeckReviewViewModel
import com.kuts.klaf.deckRepetition.DeckReviewScreen
import com.kuts.klaf.deckRepetition.RepetitionScreenState
import com.kuts.klaf.deckRepetitionInfo.DeckRepetitionInfoView
import com.kuts.klaf.deckRepetitionInfo.DeckRepetitionInfoViewModel
import com.kuts.klaf.deckRepetitionInfo.RepetitionInfoEvent
import com.kuts.klaf.deckRepetitionInfo.RepetitionInfoEvent.Non
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

private const val MIME_TYPE_TEXT_PLAIN = "text/plain"
private const val AUTHENTICATION_RESULT_KEY = "authentication_result_key"

sealed interface AppLaunchNavigationRequest {

    data object OpenDeckList : AppLaunchNavigationRequest

    data object OpenInterimCardAddition : AppLaunchNavigationRequest

    data class OpenDeckRepetition(
        val deckId: Int,
        val deckName: String,
    ) : AppLaunchNavigationRequest
}

object AppLaunchNavigationExtras {

    const val DESTINATION_KEY = "launch_destination"
    const val DECK_ID_KEY = "launch_deck_id"
    const val DECK_NAME_KEY = "launch_deck_name"

    const val DESTINATION_DECK_LIST = "deck_list"
    const val DESTINATION_INTERIM_CARD_ADDITION = "interim_card_addition"
    const val DESTINATION_DECK_REPETITION = "deck_repetition"
}

fun Intent.toAppLaunchNavigationRequest(): AppLaunchNavigationRequest? {
    val destination = getStringExtra(AppLaunchNavigationExtras.DESTINATION_KEY)

    if (destination != null) {
        return when (destination) {
            AppLaunchNavigationExtras.DESTINATION_DECK_LIST -> {
                AppLaunchNavigationRequest.OpenDeckList
            }

            AppLaunchNavigationExtras.DESTINATION_INTERIM_CARD_ADDITION -> {
                AppLaunchNavigationRequest.OpenInterimCardAddition
            }

            AppLaunchNavigationExtras.DESTINATION_DECK_REPETITION -> {
                val deckId = getIntExtra(AppLaunchNavigationExtras.DECK_ID_KEY, Deck.INTERIM_DECK_ID)
                val deckName = getStringExtra(AppLaunchNavigationExtras.DECK_NAME_KEY)
                    ?: Deck.INTERIM_DECK_NAME

                AppLaunchNavigationRequest.OpenDeckRepetition(deckId = deckId, deckName = deckName)
            }

            else -> null
        }
    }

    return if (
        action == Intent.ACTION_PROCESS_TEXT
        && type?.startsWith(MIME_TYPE_TEXT_PLAIN) == true
    ) {
        AppLaunchNavigationRequest.OpenInterimCardAddition
    } else {
        null
    }
}

@Composable
fun KlafNavHost(
    sharedViewModel: BaseMainViewModel,
    initialLaunchRequest: AppLaunchNavigationRequest?,
    launchRequests: Flow<AppLaunchNavigationRequest>,
    onRestartApp: () -> Unit,
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AppDestination.DeckList,
    ) {
        composable<AppDestination.DeckList> { backStackEntry ->
            DeckListDestination(
                navController = navController,
                backStackEntry = backStackEntry,
                sharedViewModel = sharedViewModel,
                onRestartApp = onRestartApp,
            )
        }

        dialog<AppDestination.DeckCreationDialog> { backStackEntry ->
            DeckCreationDialogDestination(
                navController = navController,
                backStackEntry = backStackEntry,
                sharedViewModel = sharedViewModel,
            )
        }

        dialog<AppDestination.DeckNavigationDialog> { backStackEntry ->
            val route = backStackEntry.toRoute<AppDestination.DeckNavigationDialog>()
            DeckNavigationDialogDestination(
                navController = navController,
                backStackEntry = backStackEntry,
                sharedViewModel = sharedViewModel,
                deckId = route.deckId,
                deckName = route.deckName,
            )
        }

        dialog<AppDestination.DeckRenamingDialog> { backStackEntry ->
            val route = backStackEntry.toRoute<AppDestination.DeckRenamingDialog>()
            DeckRenamingDialogDestination(
                navController = navController,
                backStackEntry = backStackEntry,
                sharedViewModel = sharedViewModel,
                deckId = route.deckId,
            )
        }

        dialog<AppDestination.DeckDeletingDialog> { backStackEntry ->
            val route = backStackEntry.toRoute<AppDestination.DeckDeletingDialog>()
            DeckDeletingDialogDestination(
                navController = navController,
                backStackEntry = backStackEntry,
                sharedViewModel = sharedViewModel,
                deckId = route.deckId,
                deckName = route.deckName,
            )
        }

        composable<AppDestination.DeckRepetition> { backStackEntry ->
            val route = backStackEntry.toRoute<AppDestination.DeckRepetition>()
            DeckRepetitionDestination(
                navController = navController,
                backStackEntry = backStackEntry,
                sharedViewModel = sharedViewModel,
                deckId = route.deckId,
                deckName = route.deckName,
            )
        }

        dialog<AppDestination.DeckRepetitionCardDeletingDialog> { backStackEntry ->
            val route = backStackEntry.toRoute<AppDestination.DeckRepetitionCardDeletingDialog>()
            DeckRepetitionCardDeletingDialogDestination(
                navController = navController,
                sharedViewModel = sharedViewModel,
                deckId = route.deckId,
                cardId = route.cardId,
            )
        }

        dialog<AppDestination.DeckRepetitionInfoDialog> { backStackEntry ->
            val route = backStackEntry.toRoute<AppDestination.DeckRepetitionInfoDialog>()
            DeckRepetitionInfoDialogDestination(
                navController = navController,
                sharedViewModel = sharedViewModel,
                deckId = route.deckId,
                deckName = route.deckName,
                repetitionInfoEvent = route.repetitionInfoEvent,
            )
        }

        composable<AppDestination.CardAddition> { backStackEntry ->
            val route = backStackEntry.toRoute<AppDestination.CardAddition>()
            CardAdditionDestination(
                backStackEntry = backStackEntry,
                sharedViewModel = sharedViewModel,
                deckId = route.deckId,
            )
        }

        composable<AppDestination.CardEditing> { backStackEntry ->
            val route = backStackEntry.toRoute<AppDestination.CardEditing>()
            CardEditingDestination(
                navController = navController,
                backStackEntry = backStackEntry,
                sharedViewModel = sharedViewModel,
                deckId = route.deckId,
                cardId = route.cardId,
            )
        }

        composable<AppDestination.CardViewing> { backStackEntry ->
            val route = backStackEntry.toRoute<AppDestination.CardViewing>()
            CardViewingDestination(
                sharedViewModel = sharedViewModel,
                deckId = route.deckId,
            )
        }

        dialog<AppDestination.DataSynchronizationDialog> { backStackEntry ->
            val route = backStackEntry.toRoute<AppDestination.DataSynchronizationDialog>()
            DataSynchronizationDialogDestination(
                navController = navController,
                backStackEntry = backStackEntry,
                sharedViewModel = sharedViewModel,
                authenticationActionResult = route.authenticationAction?.let { action ->
                    AuthenticationActionResult(
                        action = action,
                        isSuccessful = route.isSuccessful,
                    )
                },
            )
        }

        dialog<AppDestination.SigningTypeChoosingDialog> { backStackEntry ->
            val route = backStackEntry.toRoute<AppDestination.SigningTypeChoosingDialog>()
            SigningTypeChoosingDialogDestination(
                navController = navController,
                fromSourceDestination = route.fromSourceDestination,
            )
        }

        composable<AppDestination.Authentication> { backStackEntry ->
            val route = backStackEntry.toRoute<AppDestination.Authentication>()
            AuthenticationDestination(
                navController = navController,
                sharedViewModel = sharedViewModel,
                authenticationAction = route.authenticationAction,
                fromSourceDestination = route.fromSourceDestination,
            )
        }

        dialog<AppDestination.DrawerActionDialog> { backStackEntry ->
            val route = backStackEntry.toRoute<AppDestination.DrawerActionDialog>()
            DrawerActionDialogDestination(
                navController = navController,
                backStackEntry = backStackEntry,
                sharedViewModel = sharedViewModel,
                drawerAction = route.drawerAction,
            )
        }

        composable<AppDestination.CardTransferring> { backStackEntry ->
            val route = backStackEntry.toRoute<AppDestination.CardTransferring>()
            CardTransferringDestination(
                navController = navController,
                backStackEntry = backStackEntry,
                sharedViewModel = sharedViewModel,
                sourceDeckId = route.sourceDeckId,
            )
        }

        dialog<AppDestination.CardTransferringDeletingDialog> { backStackEntry ->
            val route = backStackEntry.toRoute<AppDestination.CardTransferringDeletingDialog>()
            CardTransferringDeletingDialogDestination(
                navController = navController,
                sharedViewModel = sharedViewModel,
                cardQuantity = route.cardQuantity,
            )
        }

        dialog<AppDestination.CardMovingDialog> {
            CardMovingDialogDestination(
                navController = navController,
                sharedViewModel = sharedViewModel,
            )
        }

        composable<AppDestination.DeckManagement> { backStackEntry ->
            val route = backStackEntry.toRoute<AppDestination.DeckManagement>()
            DeckManagementDestination(
                sharedViewModel = sharedViewModel,
                deckId = route.deckId,
            )
        }
    }

    LaunchedEffect(key1 = navController) {
        initialLaunchRequest?.let { navController.handleLaunchRequest(request = it) }
    }

    LaunchedEffect(key1 = navController, key2 = launchRequests) {
        launchRequests.collectLatest { request -> navController.handleLaunchRequest(request = request) }
    }
}

private sealed interface AppDestination {
    @Serializable
    data object DeckList : AppDestination

    @Serializable
    data object DeckCreationDialog : AppDestination

    @Serializable
    data class DeckNavigationDialog(
        val deckId: Int,
        val deckName: String,
    ) : AppDestination

    @Serializable
    data class DeckRenamingDialog(val deckId: Int) : AppDestination

    @Serializable
    data class DeckDeletingDialog(
        val deckId: Int,
        val deckName: String,
    ) : AppDestination

    @Serializable
    data class DeckRepetition(
        val deckId: Int,
        val deckName: String,
    ) : AppDestination

    @Serializable
    data class DeckRepetitionCardDeletingDialog(
        val deckId: Int,
        val cardId: Int,
    ) : AppDestination

    @Serializable
    data class DeckRepetitionInfoDialog(
        val deckId: Int,
        val deckName: String,
        val repetitionInfoEvent: RepetitionInfoEvent = Non,
    ) : AppDestination

    @Serializable
    data class CardAddition(
        val deckId: Int = Deck.INTERIM_DECK_ID,
    ) : AppDestination

    @Serializable
    data class CardEditing(
        val deckId: Int,
        val cardId: Int,
    ) : AppDestination

    @Serializable
    data class CardViewing(
        val deckId: Int,
        val deckName: String,
    ) : AppDestination

    @Serializable
    data class DataSynchronizationDialog(
        val authenticationAction: AuthenticationAction? = null,
        val isSuccessful: Boolean = false,
    ) : AppDestination

    @Serializable
    data class SigningTypeChoosingDialog(
        val fromSourceDestination: NavigationDestination,
    ) : AppDestination

    @Serializable
    data class Authentication(
        val authenticationAction: AuthenticationAction,
        val fromSourceDestination: NavigationDestination,
    ) : AppDestination

    @Serializable
    data class DrawerActionDialog(
        val drawerAction: DrawerAction,
    ) : AppDestination

    @Serializable
    data class CardTransferring(
        val sourceDeckId: Int,
    ) : AppDestination

    @Serializable
    data class CardTransferringDeletingDialog(
        val cardQuantity: Int,
    ) : AppDestination

    @Serializable
    data object CardMovingDialog : AppDestination

    @Serializable
    data class DeckManagement(
        val deckId: Int,
    ) : AppDestination
}

@Composable
private fun DeckListDestination(
    navController: NavHostController,
    backStackEntry: NavBackStackEntry,
    sharedViewModel: BaseMainViewModel,
    onRestartApp: () -> Unit,
) {
    val context = LocalContext.current

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
                val chatGptStoryCrafterPrompt = context.getString(
                    com.kuts.klaf.presentation.R.string.chat_gpt_story_crafter_prompt,
                    event.foreignWords,
                )

                context.copyToClipboard(text = chatGptStoryCrafterPrompt)
                sharedViewModel.notify(message = event.event)
                if (!context.navigateToChatGpt()) {
                    sharedViewModel.notify(
                        message = EventMessage(
                            resId = com.kuts.klaf.presentation.R.string.chat_gpt_opening_failed,
                            type = EventMessage.Type.Negative,
                        )
                    )
                }
            }

            null -> {}
        }
    }

    CollectFlowWithLifecycle(
        flow = backStackEntry.savedStateHandle.getStateFlow<AuthenticationActionResult?>(
            key = AUTHENTICATION_RESULT_KEY,
            initialValue = null,
        ),
    ) { authenticationResult ->
        if (authenticationResult == null || !authenticationResult.isSuccessful) {
            return@CollectFlowWithLifecycle
        }

        val messageId = when (authenticationResult.action) {
            AuthenticationAction.SIGN_IN -> {
                com.kuts.klaf.presentation.R.string.authentication_sign_in_success
            }

            AuthenticationAction.SIGN_UP -> {
                com.kuts.klaf.presentation.R.string.authentication_sign_up_success
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
            DeckListScreen(
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

@Composable
private fun DeckCreationDialogDestination(
    navController: NavHostController,
    backStackEntry: NavBackStackEntry,
    sharedViewModel: BaseMainViewModel,
) {
    val owner = remember(backStackEntry) {
        navController.getBackStackEntry(navController.graph.findStartDestination().id)
    }
    val viewModel: BaseDeckListViewModel = koinViewModel(viewModelStoreOwner = owner)
    val message by sharedViewModel.eventMessage.collectAsState(initial = null)

    DeckCreationDialog(
        onConfirmCreationClick = viewModel::createNewDeck,
        onCloseDialogClick = { navController.popBackStack() },
        eventMassage = message,
    )
}

@Composable
private fun DeckNavigationDialogDestination(
    navController: NavHostController,
    backStackEntry: NavBackStackEntry,
    sharedViewModel: BaseMainViewModel,
    deckId: Int,
    deckName: String,
) {
    val owner = remember(backStackEntry) {
        navController.getBackStackEntry(navController.graph.findStartDestination().id)
    }
    val viewModel: BaseDeckListViewModel = koinViewModel(viewModelStoreOwner = owner)

    DeckNavigationDialogView(
        deckName = deckName,
        eventMessage = sharedViewModel.eventMessage.collectAsState(initial = null).value,
        onDeleteDeckClick = {
            navController.navigate(
                route = AppDestination.DeckDeletingDialog(
                    deckId = deckId,
                    deckName = deckName,
                )
            )
        },
        onRenameDeckClick = {
            navController.navigate(route = AppDestination.DeckRenamingDialog(deckId = deckId))
        },
        onBrowseDeckClick = {
            navController.navigate(route = AppDestination.CardViewing(deckId = deckId, deckName = deckName))
        },
        onAddCardsClick = {
            navController.navigate(route = AppDestination.CardAddition(deckId = deckId))
        },
        onTransferCardsClick = {
            navController.navigate(route = AppDestination.CardTransferring(sourceDeckId = deckId))
        },
        onRepetitionInfoClick = {
            navController.navigate(
                route = AppDestination.DeckRepetitionInfoDialog(
                    deckId = deckId,
                    deckName = deckName,
                    repetitionInfoEvent = Non,
                )
            )
        },
        onDeckManagementClick = {
            navController.navigate(route = AppDestination.DeckManagement(deckId = deckId))
        },
        onCloseDialogClick = { navController.popBackStack() },
        onCraftStoryClick = { viewModel.generateGptPromptWithDeckContent(deckId = deckId) },
    )
}

@Composable
private fun DeckRenamingDialogDestination(
    navController: NavHostController,
    backStackEntry: NavBackStackEntry,
    sharedViewModel: BaseMainViewModel,
    deckId: Int,
) {
    val owner = remember(backStackEntry) {
        navController.getBackStackEntry(navController.graph.findStartDestination().id)
    }
    val viewModel: BaseDeckListViewModel = koinViewModel(viewModelStoreOwner = owner)
    val deck = viewModel.getDeckById(deckId = deckId)

    if (deck == null) {
        LaunchedEffect(key1 = Unit) { navController.popBackStack() }
        return
    }

    val eventMessage by sharedViewModel.eventMessage.collectAsState(initial = null)

    DeckRenamingDialog(
        deckName = deck.name,
        eventMessage = eventMessage,
        onConfirmRenamingClick = { newName ->
            viewModel.renameDeck(deck = deck, newName = newName)
        },
        onCloseDialogClick = { navController.popBackStack() },
    )
}

@Composable
private fun DeckDeletingDialogDestination(
    navController: NavHostController,
    backStackEntry: NavBackStackEntry,
    sharedViewModel: BaseMainViewModel,
    deckId: Int,
    deckName: String,
) {
    val owner = remember(backStackEntry) {
        navController.getBackStackEntry(navController.graph.findStartDestination().id)
    }
    val viewModel: BaseDeckListViewModel = koinViewModel(viewModelStoreOwner = owner)
    val eventMessage by sharedViewModel.eventMessage.collectAsState(initial = null)

    DeckDeletionDialogView(
        deckName = deckName,
        eventMessage = eventMessage,
        onCloseDialogClick = { navController.popBackStack() },
        onConfirmDeckDeletingButtonClick = { viewModel.deleteDeck(deckId = deckId) },
    )
}

@Composable
private fun DeckRepetitionDestination(
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

    Surface {
        DeckReviewScreen(
            viewModel = viewModel,
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
                navController.navigate(route = AppDestination.CardEditing(deckId = deckId, cardId = cardId))
            },
        )
    }
}

@Composable
private fun DeckRepetitionCardDeletingDialogDestination(
    navController: NavHostController,
    sharedViewModel: BaseMainViewModel,
    deckId: Int,
    cardId: Int,
) {
    val owner = navController.previousBackStackEntry ?: return

    val viewModel: BaseDeckReviewViewModel = koinViewModel(viewModelStoreOwner = owner)

    CollectFlowWithLifecycle(flow = viewModel.cardDeletingState) { deletingState ->
        if (deletingState is LoadingState.Success) {
            navController.popBackStack()
        }
    }

    val eventMessage by sharedViewModel.eventMessage.collectAsState(initial = null)

    CardDeletingDialogView(
        cardQuantity = 1,
        eventMessage = eventMessage,
        onConfirmDeleting = { viewModel.deleteCard(cardId = cardId, deckId = deckId) },
        onCancel = { navController.popBackStack() },
    )
}

@Composable
private fun DeckRepetitionInfoDialogDestination(
    navController: NavHostController,
    sharedViewModel: BaseMainViewModel,
    deckId: Int,
    deckName: String,
    repetitionInfoEvent: RepetitionInfoEvent,
) {
    val viewModel: DeckRepetitionInfoViewModel = koinViewModel(
        parameters = { parametersOf(deckId) }
    )

    CollectFlowWithLifecycle(flow = viewModel.eventMessage) { eventMessage ->
        sharedViewModel.notify(message = eventMessage)
        navController.popBackStack()
    }

    var isRepetitionInfoEventHandled by remember(repetitionInfoEvent) {
        mutableStateOf(value = false)
    }

    DeckRepetitionInfoView(
        viewModel = viewModel,
        deckName = deckName,
        onCloseClick = { navController.popBackStack() },
        eventMessage = sharedViewModel.eventMessage.collectAsState(initial = null).value,
        onRendered = {
            if (isRepetitionInfoEventHandled) {
                return@DeckRepetitionInfoView
            }

            val eventMessage = when (repetitionInfoEvent) {
                RepetitionInfoEvent.ScheduledSuccessfully -> {
                    EventMessage(
                        resId = com.kuts.klaf.presentation.R.string.deck_repetition_scheduled_successfully,
                        type = EventMessage.Type.Positive,
                    )
                }

                RepetitionInfoEvent.SchedulingFailed -> {
                    EventMessage(
                        resId = com.kuts.klaf.presentation.R.string.deck_repetition_scheduling_failed,
                        type = EventMessage.Type.Negative,
                    )
                }

                RepetitionInfoEvent.OneRepetitionToFinish -> {
                    EventMessage(resId = com.kuts.klaf.presentation.R.string.deck_repetition_one_repetition_to_finish_iteration)
                }

                Non -> null
            }

            eventMessage?.let { sharedViewModel.notify(message = it) }
            isRepetitionInfoEventHandled = true
        },
    )
}

@Composable
private fun CardAdditionDestination(
    backStackEntry: NavBackStackEntry,
    sharedViewModel: BaseMainViewModel,
    deckId: Int,
) {
    val context = LocalContext.current

    val viewModel: CardAdditionViewModel = koinViewModel(
        viewModelStoreOwner = backStackEntry,
        parameters = {
            parametersOf(
                deckId,
                context.retrieveSmartSelectedWord(),
            )
        },
    )

    ObserveAudioLifecycle(
        onCreate = viewModel.audioPlayer::onCreate,
        onResume = viewModel.audioPlayer::onResume,
        onStop = viewModel.audioPlayer::onStop,
        onDestroy = viewModel.audioPlayer::onDestroy,
    )

    CollectFlowWithLifecycle(flow = viewModel.eventMessage, onEach = sharedViewModel::notify)

    Surface {
        CardManagementScreen(viewModel = viewModel)
    }
}

@Composable
private fun CardEditingDestination(
    navController: NavHostController,
    backStackEntry: NavBackStackEntry,
    sharedViewModel: BaseMainViewModel,
    deckId: Int,
    cardId: Int,
) {
    val viewModel: CardEditingViewModel = koinViewModel(
        viewModelStoreOwner = backStackEntry,
        parameters = { parametersOf(deckId, cardId) },
    )

    ObserveAudioLifecycle(
        onCreate = viewModel.audioPlayer::onCreate,
        onResume = viewModel.audioPlayer::onResume,
        onStop = viewModel.audioPlayer::onStop,
        onDestroy = viewModel.audioPlayer::onDestroy,
    )

    CollectFlowWithLifecycle(flow = viewModel.eventMessage, onEach = sharedViewModel::notify)

    CollectFlowWithLifecycle(flow = viewModel.cardManagementState) { managementState ->
        if (managementState is CardManagementState.Finished) {
            navController.popBackStack()
        }
    }

    Surface {
        CardManagementScreen(viewModel = viewModel)
    }
}

@Composable
private fun CardViewingDestination(
    sharedViewModel: BaseMainViewModel,
    deckId: Int,
) {
    val viewModel: CardViewingViewModel = koinViewModel(parameters = { parametersOf(deckId) })

    CollectFlowWithLifecycle(flow = viewModel.eventMessage, onEach = sharedViewModel::notify)

    Surface {
        CardViewingScreen(viewModel = viewModel)
    }
}

@Composable
private fun DataSynchronizationDialogDestination(
    navController: NavHostController,
    backStackEntry: NavBackStackEntry,
    sharedViewModel: BaseMainViewModel,
    authenticationActionResult: AuthenticationActionResult?,
) {
    val owner = remember(backStackEntry) {
        navController.getBackStackEntry(navController.graph.findStartDestination().id)
    }
    val viewModel: BaseDeckListViewModel = koinViewModel(viewModelStoreOwner = owner)

    val eventMessage by sharedViewModel.eventMessage.collectAsState(initial = null)

    DataSynchronizationDialogView(
        synchronizationState = viewModel.dataSynchronizationState.collectAsState().value,
        onConfirmClick = viewModel::synchronizeData,
        onCloseClick = { navController.popBackStack() },
        onDispose = viewModel::resetSynchronizationState,
        eventMassage = eventMessage,
        onLaunched = {
            if (authenticationActionResult?.isSuccessful != true) {
                return@DataSynchronizationDialogView
            }

            val messageId = when (authenticationActionResult.action) {
                AuthenticationAction.SIGN_IN -> {
                    com.kuts.klaf.presentation.R.string.authentication_sign_in_success
                }

                AuthenticationAction.SIGN_UP -> {
                    com.kuts.klaf.presentation.R.string.authentication_sign_up_success
                }
            }

            sharedViewModel.notify(
                message = EventMessage(
                    resId = messageId,
                    type = EventMessage.Type.Positive,
                )
            )
        },
    )
}

@Composable
private fun SigningTypeChoosingDialogDestination(
    navController: NavHostController,
    fromSourceDestination: NavigationDestination,
) {
    SigningTypeChoosingView(
        fromSourceDestination = fromSourceDestination,
        onSigningActionButtonClick = { action ->
            val currentDestinationId = navController.currentDestination?.id

            navController.navigate(
                route = AppDestination.Authentication(
                    authenticationAction = action,
                    fromSourceDestination = fromSourceDestination,
                )
            ) {
                currentDestinationId?.let { destinationId ->
                    popUpTo(id = destinationId) { inclusive = true }
                }
            }
        },
        onCloseButtonClick = { navController.popBackStack() },
    )
}

@Composable
private fun AuthenticationDestination(
    navController: NavHostController,
    sharedViewModel: BaseMainViewModel,
    authenticationAction: AuthenticationAction,
    fromSourceDestination: NavigationDestination,
) {
    val viewModel: BaseAuthenticationViewModel = koinViewModel()

    CollectFlowWithLifecycle(flow = viewModel.eventMessage, onEach = sharedViewModel::notify)

    Surface {
        AuthenticationScreen(
            action = authenticationAction,
            viewModel = viewModel,
            onAuthenticationFinished = { finishedAction ->
                when (fromSourceDestination) {
                    NavigationDestination.DECK_LIST_FRAGMENT -> {
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set(
                                key = AUTHENTICATION_RESULT_KEY,
                                value = AuthenticationActionResult(
                                    action = finishedAction,
                                    isSuccessful = true,
                                )
                            )

                        navController.popBackStack()
                    }

                    NavigationDestination.DATA_SYNCHRONIZATION_DIALOG -> {
                        navController.navigate(
                            route = AppDestination.DataSynchronizationDialog(
                                authenticationAction = finishedAction,
                                isSuccessful = true,
                            )
                        ) {
                            popUpTo(id = navController.graph.findStartDestination().id) {
                                inclusive = false
                            }
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun DrawerActionDialogDestination(
    navController: NavHostController,
    backStackEntry: NavBackStackEntry,
    sharedViewModel: BaseMainViewModel,
    drawerAction: DrawerAction,
) {
    val owner = remember(backStackEntry) {
        navController.getBackStackEntry(navController.graph.findStartDestination().id)
    }
    val viewModel: BaseDeckListViewModel = koinViewModel(viewModelStoreOwner = owner)

    val eventMessage = sharedViewModel.eventMessage.collectAsState(initial = null).value

    DrawerActionView(
        action = drawerAction,
        loadingState = viewModel.drawerActionLoadingState.collectAsState().value,
        onCloseDialog = { navController.popBackStack() },
        onConfirmationClick = {
            when (drawerAction) {
                DrawerAction.LOG_OUT -> viewModel.logOut()
                DrawerAction.DELETE_ACCOUNT -> viewModel.deleteAccount()
            }
        },
        eventMessage = eventMessage,
    )
}

@Composable
private fun CardTransferringDestination(
    navController: NavHostController,
    backStackEntry: NavBackStackEntry,
    sharedViewModel: BaseMainViewModel,
    sourceDeckId: Int,
) {
    val viewModel: BaseCardTransferringViewModel = koinViewModel(
        viewModelStoreOwner = backStackEntry,
        parameters = { parametersOf(sourceDeckId) },
    )

    ObserveAudioLifecycle(
        onCreate = viewModel.audioPlayer::onCreate,
        onResume = viewModel.audioPlayer::onResume,
        onStop = viewModel.audioPlayer::onStop,
        onDestroy = viewModel.audioPlayer::onDestroy,
    )

    CollectFlowWithLifecycle(flow = viewModel.eventMessage, onEach = sharedViewModel::notify)

    CollectFlowWithLifecycle(flow = viewModel.navigationEvent) { event ->
        when (event) {
            is ICardTransferringNavigationEvent.ToCardEditingScreen -> {
                navController.navigate(
                    route = AppDestination.CardEditing(
                        deckId = event.deckId,
                        cardId = event.cardId,
                    )
                )
            }

            ICardTransferringNavigationEvent.ToCardMovingDialog -> {
                navController.navigate(route = AppDestination.CardMovingDialog)
            }

            is ICardTransferringNavigationEvent.ToCardAddingScreen -> {
                navController.navigate(route = AppDestination.CardAddition(deckId = event.sourceDeckId))
            }

            is ICardTransferringNavigationEvent.ToCardDeletingDialog -> {
                navController.navigate(
                    route = AppDestination.CardTransferringDeletingDialog(
                        cardQuantity = event.cardQuantity
                    )
                )
            }

            ICardTransferringNavigationEvent.ToPrevious -> {
                navController.popBackStack()
            }
        }
    }

    Surface {
        CardTransferringScreen(viewModel = viewModel)
    }
}

@Composable
private fun CardTransferringDeletingDialogDestination(
    navController: NavHostController,
    sharedViewModel: BaseMainViewModel,
    cardQuantity: Int,
) {
    val owner = navController.previousBackStackEntry ?: return

    val viewModel: BaseCardTransferringViewModel = koinViewModel(viewModelStoreOwner = owner)

    CardDeletingDialogView(
        cardQuantity = cardQuantity,
        onConfirmDeleting = {
            viewModel.sendAction(action = ICardTransferringAction.DeleteCards)
        },
        onCancel = {
            viewModel.sendAction(
                action = ICardTransferringAction.NavigateTo(destination = CardTransferringScreenDestination)
            )
        },
        eventMessage = sharedViewModel.eventMessage.collectAsState(initial = null).value,
    )
}

@Composable
private fun CardMovingDialogDestination(
    navController: NavHostController,
    sharedViewModel: BaseMainViewModel,
) {
    val owner = navController.previousBackStackEntry ?: return

    val viewModel: BaseCardTransferringViewModel = koinViewModel(viewModelStoreOwner = owner)

    DeckChoosingDialogView(
        decks = viewModel.decks.collectAsState().value,
        onConfirmClick = { targetDeck ->
            viewModel.sendAction(action = ICardTransferringAction.MoveCards(targetDeck = targetDeck))
        },
        onCloseClick = {
            viewModel.sendAction(
                action = ICardTransferringAction.NavigateTo(destination = CardTransferringScreenDestination)
            )
        },
        eventMessage = sharedViewModel.eventMessage.collectAsState(initial = null).value,
    )
}

@Composable
private fun DeckManagementDestination(
    sharedViewModel: BaseMainViewModel,
    deckId: Int,
) {
    val viewModel: BaseDeckManagementViewModel = koinViewModel(parameters = { parametersOf(deckId) })

    CollectFlowWithLifecycle(flow = viewModel.eventMessage, onEach = sharedViewModel::notify)

    Surface {
        DeckManagementScreen(
            deckManagementState = viewModel.deckManagementState.collectAsState().value,
            sendAction = viewModel::sendAction,
        )
    }
}

@Composable
private fun ObserveAudioLifecycle(
    onCreate: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onDestroy: () -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(key1 = lifecycleOwner) {
        onCreate()

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> onResume()
                Lifecycle.Event.ON_STOP -> onStop()
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            onDestroy()
        }
    }
}

@Composable
private fun <T> CollectFlowWithLifecycle(
    flow: Flow<T>,
    onEach: (T) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(key1 = flow, key2 = lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(state = Lifecycle.State.STARTED) {
            flow.collect { value -> onEach(value) }
        }
    }
}

private fun NavHostController.handleLaunchRequest(request: AppLaunchNavigationRequest) {
    when (request) {
        AppLaunchNavigationRequest.OpenDeckList -> {
            navigate(route = AppDestination.DeckList) {
                popUpTo(id = graph.findStartDestination().id) {
                    inclusive = false
                }
                launchSingleTop = true
            }
        }

        AppLaunchNavigationRequest.OpenInterimCardAddition -> {
            navigate(route = AppDestination.CardAddition(deckId = Deck.INTERIM_DECK_ID)) {
                popUpTo(id = graph.findStartDestination().id) {
                    inclusive = false
                }
                launchSingleTop = true
            }
        }

        is AppLaunchNavigationRequest.OpenDeckRepetition -> {
            navigate(
                route = AppDestination.DeckRepetition(
                    deckId = request.deckId,
                    deckName = request.deckName,
                )
            ) {
                popUpTo(id = graph.findStartDestination().id) {
                    inclusive = false
                }
                launchSingleTop = true
            }
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
    }.getOrDefault(false)
}

private fun Context.retrieveSmartSelectedWord(): String? {
    val activity = this as? Activity ?: return null

    val intent = activity.intent ?: return null
    if (intent.action != Intent.ACTION_PROCESS_TEXT) {
        return null
    }

    val selectedWord = intent.type
        ?.startsWith(MIME_TYPE_TEXT_PLAIN)
        ?.ifTrue { intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString() }

    if (selectedWord != null) {
        intent.removeExtra(Intent.EXTRA_PROCESS_TEXT)
        intent.action = Intent.ACTION_MAIN
    }

    return selectedWord
}
