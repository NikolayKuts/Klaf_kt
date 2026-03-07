package com.kuts.klaf.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.kuts.domain.entities.Deck
import com.kuts.klaf.common.externalActions.IExternalAppActions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

internal typealias DestinationsRegistration<SharedViewModel> = NavGraphBuilder.(
    navController: NavHostController,
    sharedViewModel: SharedViewModel,
    externalAppActions: IExternalAppActions,
    onRestartApp: () -> Unit,
) -> Unit

@Composable
internal fun <SharedViewModel> KlafNavHostContent(
    sharedViewModel: SharedViewModel,
    externalAppActions: IExternalAppActions,
    initialLaunchRequest: AppLaunchNavigationRequest?,
    launchRequests: Flow<AppLaunchNavigationRequest>,
    onRestartApp: () -> Unit,
    registerDestinations: DestinationsRegistration<SharedViewModel>,
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AppDestination.DeckList,
    ) {
        registerDestinations(
            navController,
            sharedViewModel,
            externalAppActions,
            onRestartApp,
        )
    }

    LaunchedEffect(key1 = navController) {
        initialLaunchRequest?.let { navController.handleLaunchRequest(request = it) }
    }

    LaunchedEffect(key1 = navController, key2 = launchRequests) {
        launchRequests.collectLatest { request -> navController.handleLaunchRequest(request = request) }
    }
}

internal fun NavHostController.handleLaunchRequest(request: AppLaunchNavigationRequest) {
    when (request) {
        AppLaunchNavigationRequest.OpenDeckList -> {
            navigate(route = AppDestination.DeckList) {
                popUpTo(route = AppDestination.DeckList) {
                    inclusive = false
                }
                launchSingleTop = true
            }
        }

        AppLaunchNavigationRequest.OpenInterimCardAddition -> {
            navigate(route = AppDestination.CardAddition(deckId = Deck.INTERIM_DECK_ID)) {
                popUpTo(route = AppDestination.DeckList) {
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
                popUpTo(route = AppDestination.DeckList) {
                    inclusive = false
                }
                launchSingleTop = true
            }
        }
    }
}
