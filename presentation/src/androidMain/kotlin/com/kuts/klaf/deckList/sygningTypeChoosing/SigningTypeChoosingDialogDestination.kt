package com.kuts.klaf.deckList.sygningTypeChoosing

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.kuts.klaf.common.NavigationDestination
import com.kuts.klaf.navigation.AppDestination

@Composable
internal fun SigningTypeChoosingDialogDestination(
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
