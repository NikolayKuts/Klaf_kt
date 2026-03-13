package com.kuts.klaf

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import com.kuts.klaf.common.BaseMainViewModel
import com.kuts.klaf.common.EventMessageView
import com.kuts.klaf.common.MainViewModel
import com.kuts.klaf.navigation.IosKlafNavHost
import com.kuts.klaf.theme.MainTheme
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController {
    MainTheme {
        IosRootContent()
    }
}

@Composable
private fun IosRootContent() {
    val sharedViewModel: BaseMainViewModel = remember { MainViewModel() }
    val eventMessage by sharedViewModel.eventMessage.collectAsState(initial = null)

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        IosKlafNavHost(
            sharedViewModel = sharedViewModel,
            onRestartApp = {},
        )

        eventMessage?.let { message ->
            EventMessageView(
                modifier = Modifier.align(Alignment.TopCenter),
                message = message,
            )
        }
    }
}
