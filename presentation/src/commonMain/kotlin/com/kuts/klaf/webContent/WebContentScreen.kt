package com.kuts.klaf.webContent

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun WebContentScreen(
    navController: NavHostController,
    backStackEntry: NavBackStackEntry,
    source: WebContentSource,
) {
    val viewModel: WebContentViewModel = koinViewModel(
        viewModelStoreOwner = backStackEntry,
        parameters = { parametersOf(source) },
    )
    val uiState by viewModel.uiState.collectAsState()

    Surface(modifier = Modifier.fillMaxSize()) {
        uiState.url?.let { resolvedUrl ->
            WebContentContent(
                url = resolvedUrl,
                config = uiState.config,
                modifier = Modifier.fillMaxSize(),
                onNavigationRequest = viewModel::resolveNavigationRequest,
                onPageLoadStarted = viewModel::onPageLoadStarted,
                onPageLoadFinished = viewModel::onPageLoadFinished,
                onPageLoadProgressChanged = viewModel::onPageLoadProgressChanged,
                onPageLoadError = viewModel::onPageLoadError,
                onCloseRequest = navController::popBackStack,
            )
        }
    }
}

@Composable
private fun WebContentContent(
    url: String,
    config: WebContentConfig,
    modifier: Modifier = Modifier,
    onNavigationRequest: (String?) -> String?,
    onPageLoadStarted: (String?) -> Unit,
    onPageLoadFinished: (String?) -> Unit,
    onPageLoadProgressChanged: (Int) -> Unit,
    onPageLoadError: (String?) -> Unit,
    onCloseRequest: () -> Unit,
) {
    PlatformWebContentView(
        url = url,
        config = config,
        modifier = modifier,
        onNavigationRequest = onNavigationRequest,
        onPageLoadStarted = onPageLoadStarted,
        onPageLoadFinished = onPageLoadFinished,
        onPageLoadProgressChanged = onPageLoadProgressChanged,
        onPageLoadError = onPageLoadError,
        onCloseRequest = onCloseRequest,
    )
}
