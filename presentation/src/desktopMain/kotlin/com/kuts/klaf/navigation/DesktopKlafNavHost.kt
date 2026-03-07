package com.kuts.klaf.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.kuts.klaf.common.BaseMainViewModel
import com.kuts.klaf.common.externalActions.DesktopExternalAppActions
import kotlinx.coroutines.flow.emptyFlow

@Composable
fun DesktopKlafNavHost(
    sharedViewModel: BaseMainViewModel,
    onRestartApp: () -> Unit,
) {
    val externalAppActions = remember { DesktopExternalAppActions() }

    KlafNavHostContent(
        sharedViewModel = sharedViewModel,
        externalAppActions = externalAppActions,
        initialLaunchRequest = null,
        launchRequests = emptyFlow(),
        onRestartApp = onRestartApp,
        registerDestinations = { navController, hostSharedViewModel, hostExternalAppActions, restartApp ->
            registerAppDestinations(
                navController = navController,
                sharedViewModel = hostSharedViewModel,
                externalAppActions = hostExternalAppActions,
                onRestartApp = restartApp,
            )
        },
    )
}
