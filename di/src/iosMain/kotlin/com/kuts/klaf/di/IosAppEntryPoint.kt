package com.kuts.klaf.di

import com.kuts.klaf.MainViewController
import org.koin.core.context.startKoin
import platform.UIKit.UIViewController

fun IosAppEntryPoint(): UIViewController {
    startIosKoin()
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
