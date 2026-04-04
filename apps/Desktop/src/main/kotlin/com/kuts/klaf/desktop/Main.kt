package com.kuts.klaf.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.kuts.klaf.common.BaseMainViewModel
import com.kuts.klaf.common.EventMessageView
import com.kuts.klaf.common.MainViewModel
import com.kuts.klaf.di.appModules
import com.kuts.klaf.navigation.DesktopKlafNavHost
import com.kuts.klaf.theme.MainTheme
import com.lib.lokdroid.core.LoKdroid
import com.lib.lokdroid.core.log
import com.lib.lokdroid.data.default_implementation.FormatterBuilder
import org.koin.core.context.startKoin

fun main() = application {
    startDesktopKoin()

    LoKdroid.initialize(
        formatter = FormatterBuilder().withPointer()
            .space()
            .withLineReference()
            .space()
            .message()
            .build()
    )

    log {
        "init"()
        "Desktop"(I)
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Klaf Desktop",
    ) {
        MainTheme {
            DesktopRootContent()
        }
    }
}

@Composable
private fun DesktopRootContent() {
    val sharedViewModel: BaseMainViewModel = remember { MainViewModel() }
    val eventMessage by sharedViewModel.eventMessage.collectAsState(initial = null)

    Box(modifier = Modifier.fillMaxSize()) {
        DesktopKlafNavHost(
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

private fun startDesktopKoin() {
    if (org.koin.core.context.GlobalContext.getOrNull() == null) {
        startKoin {
            modules(appModules)
        }
    }
}
