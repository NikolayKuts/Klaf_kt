package com.kuts.klaf

import android.app.Application
import com.kuts.klaf.di.appModules
import com.lib.lokdroid.core.LoKdroid
import com.lib.lokdroid.data.default_implementation.FormatterBuilder
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.startKoin

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@App)
            workManagerFactory()
            modules(appModules)
        }

        LoKdroid.initialize(
            formatter = FormatterBuilder().withPointer()
                .space()
                .withLineReference()
                .space()
                .message()
                .build()
        )
    }
}
