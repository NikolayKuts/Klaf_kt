package com.kuts.klaf.deckList.common

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.kuts.domain.common.AuthenticationAction
import com.kuts.klaf.authentication.AuthenticationActionResult
import com.kuts.klaf.common.BaseMainViewModel
import com.kuts.klaf.common.EventMessage
import com.kuts.klaf.common.NavigationDestination
import com.kuts.klaf.common.SecretConstants
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
import com.kuts.klaf.presentation.R
import com.lib.lokdroid.core.logE
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun DeckListDestination(
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
                    R.string.chat_gpt_story_crafter_prompt,
                    event.foreignWords,
                )

                context.copyToClipboard(text = chatGptStoryCrafterPrompt)
                sharedViewModel.notify(message = event.event)
                if (!context.navigateToChatGpt()) {
                    sharedViewModel.notify(
                        message = EventMessage(
                            resId = R.string.chat_gpt_opening_failed,
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
                R.string.authentication_sign_in_success
            }

            AuthenticationAction.SIGN_UP -> {
                R.string.authentication_sign_up_success
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
