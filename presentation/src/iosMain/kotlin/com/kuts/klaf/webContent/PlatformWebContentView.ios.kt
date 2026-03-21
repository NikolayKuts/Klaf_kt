package com.kuts.klaf.webContent

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun PlatformWebContentView(
    url: String,
    config: WebContentConfig,
    modifier: Modifier,
    onNavigationRequest: (String?) -> String?,
    onPageLoadStarted: (String?) -> Unit,
    onPageLoadFinished: (String?) -> Unit,
    onPageLoadProgressChanged: (Int) -> Unit,
    onPageLoadError: (String?) -> Unit,
) {
    TODO("iOS web content view is not implemented yet")
}
