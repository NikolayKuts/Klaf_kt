package com.kuts.klaf.di

import com.kuts.klaf.MainViewController
import com.lib.lokdroid.core.LoKdroid
import com.lib.lokdroid.core.log
import com.lib.lokdroid.data.default_implementation.FormatterBuilder
import org.koin.core.context.startKoin
import platform.UIKit.UIViewController

fun IosAppEntryPoint(): UIViewController {
    startIosKoin()
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
        "iOS"(I)
    }
    return MainViewController()
}

private var isKoinStarted = false

private fun startIosKoin() {
    if (isKoinStarted) return

    startKoin {
        modules(appModules)
    }
    isKoinStarted = true
}
