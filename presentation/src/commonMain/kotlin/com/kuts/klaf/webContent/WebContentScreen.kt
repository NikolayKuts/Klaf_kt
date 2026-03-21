package com.kuts.klaf.webContent

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@Composable
internal fun WebContentScreen(initialUrl: String) {
    val viewModel = remember(initialUrl) {
        WebContentViewModel(initialUrl = initialUrl)
    }
    val url by viewModel.url.collectAsState()

    Surface(modifier = Modifier.fillMaxSize()) {
        WebContentContent(
            url = url,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun WebContentContent(
    url: String,
    modifier: Modifier = Modifier,
) {
    PlatformWebContentView(
        url = url,
        modifier = modifier,
    )
}
