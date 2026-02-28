package com.kuts.klaf

import android.app.Application
import com.kuts.klaf.di.appModules
import com.lib.lokdroid.core.LoKdroid
import com.lib.lokdroid.data.default_implementation.FormaterBuilder
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
//        setupWorkManagerFactory()


        LoKdroid.initialize(
            formatter = FormaterBuilder().withPointer()
                .space()
                .withLineReference()
                .space()
                .message()
                .build()
        )
    }
}
