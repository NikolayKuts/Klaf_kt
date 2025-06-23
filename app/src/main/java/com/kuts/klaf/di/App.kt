package com.kuts.klaf.di

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.lib.lokdroid.core.LoKdroid
import com.lib.lokdroid.data.default_implementation.FormaterBuilder
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class App : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var loKDroid: LoKdroid

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()

        loKDroid.initialize(
            formatter = FormaterBuilder().withPointer()
                .space()
                .withLineReference()
                .space()
                .message()
                .build()
        )
    }
}