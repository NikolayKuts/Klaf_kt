package com.kuts.klaf.navigation

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.kuts.domain.entities.Deck
import com.kuts.klaf.authentication.AuthenticationActionResult
import com.kuts.klaf.authentication.AuthenticationDestination
import com.kuts.klaf.cardManagement.cardAddition.CardAdditionDestination
import com.kuts.klaf.cardManagement.cardEditing.CardEditingDestination
import com.kuts.klaf.cardTransferring.cardDeleting.CardTransferringDeletingDialogDestination
import com.kuts.klaf.cardTransferring.common.CardTransferringDestination
import com.kuts.klaf.cardTransferring.deckChoosing.CardMovingDialogDestination
import com.kuts.klaf.cardViewing.CardViewingDestination
import com.kuts.klaf.common.BaseMainViewModel
import com.kuts.klaf.deckList.common.DeckListDestination
import com.kuts.klaf.deckList.dataSynchronization.DataSynchronizationDialogDestination
import com.kuts.klaf.deckList.deckCreation.DeckCreationDialogDestination
import com.kuts.klaf.deckList.deckDeleting.DeckDeletingDialogDestination
import com.kuts.klaf.deckList.deckNavigation.DeckNavigationDialogDestination
import com.kuts.klaf.deckList.deckRenaming.DeckRenamingDialogDestination
import com.kuts.klaf.deckList.drawer.DrawerActionDialogDestination
import com.kuts.klaf.deckList.sygningTypeChoosing.SigningTypeChoosingDialogDestination
import com.kuts.klaf.deckManagment.DeckManagementDestination
import com.kuts.klaf.deckRepetition.DeckRepetitionDestination
import com.kuts.klaf.deckRepetition.cardDeleting.DeckRepetitionCardDeletingDialogDestination
import com.kuts.klaf.deckRepetitionInfo.DeckRepetitionInfoDialogDestination
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

private const val MIME_TYPE_TEXT_PLAIN = "text/plain"

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
        buildComposableWithEntry<AppDestination.DeckList> { backStackEntry ->
            DeckListDestination(
                navController = navController,
                backStackEntry = backStackEntry,
                sharedViewModel = sharedViewModel,
                onRestartApp = onRestartApp,
            )
        }

        buidDialogWithEntry<AppDestination.DeckCreationDialog> { backStackEntry ->
            DeckCreationDialogDestination(
                navController = navController,
                backStackEntry = backStackEntry,
                sharedViewModel = sharedViewModel,
            )
        }

        buildDialog<AppDestination.DeckNavigationDialog> { backStackEntry, route ->
            DeckNavigationDialogDestination(
                navController = navController,
                backStackEntry = backStackEntry,
                sharedViewModel = sharedViewModel,
                deckId = route.deckId,
                deckName = route.deckName,
            )
        }

        buildDialog<AppDestination.DeckRenamingDialog> { backStackEntry, route ->
            DeckRenamingDialogDestination(
                navController = navController,
                backStackEntry = backStackEntry,
                sharedViewModel = sharedViewModel,
                deckId = route.deckId,
            )
        }

        buildDialog<AppDestination.DeckDeletingDialog> { backStackEntry, route ->
            DeckDeletingDialogDestination(
                navController = navController,
                backStackEntry = backStackEntry,
                sharedViewModel = sharedViewModel,
                deckId = route.deckId,
                deckName = route.deckName,
            )
        }

        buildComposable<AppDestination.DeckRepetition> { backStackEntry, route ->
            DeckRepetitionDestination(
                navController = navController,
                backStackEntry = backStackEntry,
                sharedViewModel = sharedViewModel,
                deckId = route.deckId,
                deckName = route.deckName,
            )
        }

        buildDialog<AppDestination.DeckRepetitionCardDeletingDialog> { route ->
            DeckRepetitionCardDeletingDialogDestination(
                navController = navController,
                sharedViewModel = sharedViewModel,
                deckId = route.deckId,
                cardId = route.cardId,
            )
        }

        buildDialog<AppDestination.DeckRepetitionInfoDialog> { route ->
            DeckRepetitionInfoDialogDestination(
                navController = navController,
                sharedViewModel = sharedViewModel,
                deckId = route.deckId,
                deckName = route.deckName,
                repetitionInfoEvent = route.repetitionInfoEvent,
            )
        }

        buildComposable<AppDestination.CardAddition> { backStackEntry, route ->
            CardAdditionDestination(
                backStackEntry = backStackEntry,
                sharedViewModel = sharedViewModel,
                context = navController.context,
                deckId = route.deckId,
            )
        }

        buildComposable<AppDestination.CardEditing> { backStackEntry, route ->
            CardEditingDestination(
                navController = navController,
                backStackEntry = backStackEntry,
                sharedViewModel = sharedViewModel,
                deckId = route.deckId,
                cardId = route.cardId,
            )
        }

        buildComposable<AppDestination.CardViewing> { route ->
            CardViewingDestination(
                sharedViewModel = sharedViewModel,
                deckId = route.deckId,
            )
        }

        buildDialog<AppDestination.DataSynchronizationDialog> { backStackEntry, route ->
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

        buildDialog<AppDestination.SigningTypeChoosingDialog> { route ->
            SigningTypeChoosingDialogDestination(
                navController = navController,
                fromSourceDestination = route.fromSourceDestination,
            )
        }

        buildComposable<AppDestination.Authentication> { route ->
            AuthenticationDestination(
                navController = navController,
                sharedViewModel = sharedViewModel,
                authenticationAction = route.authenticationAction,
                fromSourceDestination = route.fromSourceDestination,
            )
        }

        buildDialog<AppDestination.DrawerActionDialog> { backStackEntry, route ->
            DrawerActionDialogDestination(
                navController = navController,
                backStackEntry = backStackEntry,
                sharedViewModel = sharedViewModel,
                drawerAction = route.drawerAction,
            )
        }

        buildComposable<AppDestination.CardTransferring> { backStackEntry, route ->
            CardTransferringDestination(
                navController = navController,
                backStackEntry = backStackEntry,
                sharedViewModel = sharedViewModel,
                sourceDeckId = route.sourceDeckId,
            )
        }

        buildDialog<AppDestination.CardTransferringDeletingDialog> { route ->
            CardTransferringDeletingDialogDestination(
                navController = navController,
                sharedViewModel = sharedViewModel,
                cardQuantity = route.cardQuantity,
            )
        }

        buidDialogWithEntry<AppDestination.CardMovingDialog> {
            CardMovingDialogDestination(
                navController = navController,
                sharedViewModel = sharedViewModel,
            )
        }

        buildComposable<AppDestination.DeckManagement> { route ->
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
