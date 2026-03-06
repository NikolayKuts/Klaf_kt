package com.kuts.klaf.authentication

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.kuts.domain.common.AuthenticationAction
import com.kuts.klaf.common.BaseMainViewModel
import com.kuts.klaf.common.NavigationDestination
import com.kuts.klaf.navigation.AUTHENTICATION_RESULT_KEY
import com.kuts.klaf.navigation.AppDestination
import com.kuts.klaf.navigation.CollectFlowWithLifecycle
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun AuthenticationDestination(
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
                                value = Json.encodeToString(
                                    AuthenticationActionResult(
                                        action = finishedAction,
                                        isSuccessful = true,
                                    )
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
