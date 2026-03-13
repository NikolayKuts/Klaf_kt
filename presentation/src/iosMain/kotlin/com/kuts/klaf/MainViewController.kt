package com.kuts.klaf

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import com.kuts.klaf.theme.MainTheme
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController {
    MainTheme {
        IosEntryScreen()
    }
}

@Composable
private fun IosEntryScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "Klaf iOS entry point")
    }
}
