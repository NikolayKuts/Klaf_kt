package com.kuts.klaf.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.kuts.klaf.common.BaseMainViewModel
import com.kuts.klaf.common.externalActions.AndroidExternalAppActions
import kotlinx.coroutines.flow.Flow

@Composable
fun AndroidKlafNavHost(
    sharedViewModel: BaseMainViewModel,
    initialLaunchRequest: AppLaunchNavigationRequest?,
    launchRequests: Flow<AppLaunchNavigationRequest>,
    onRestartApp: () -> Unit,
) {
    val context = LocalContext.current
    val externalAppActions = remember(context) {
        AndroidExternalAppActions(context = context)
    }

    KlafNavHostContent(
        sharedViewModel = sharedViewModel,
        externalAppActions = externalAppActions,
        initialLaunchRequest = initialLaunchRequest,
        launchRequests = launchRequests,
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
