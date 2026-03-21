package com.kuts.klaf.webContent

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun PlatformWebContentView(
    url: String,
    config: WebContentConfig,
    modifier: Modifier = Modifier,
    onNavigationRequest: (String?) -> String? = { it },
    onPageLoadStarted: (String?) -> Unit = {},
    onPageLoadFinished: (String?) -> Unit = {},
    onPageLoadProgressChanged: (Int) -> Unit = {},
    onPageLoadError: (String?) -> Unit = {},
    onCloseRequest: () -> Unit = {},
)
