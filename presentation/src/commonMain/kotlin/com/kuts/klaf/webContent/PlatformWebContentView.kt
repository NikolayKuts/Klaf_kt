package com.kuts.klaf.webContent

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun PlatformWebContentView(
    url: String,
    modifier: Modifier = Modifier,
)
