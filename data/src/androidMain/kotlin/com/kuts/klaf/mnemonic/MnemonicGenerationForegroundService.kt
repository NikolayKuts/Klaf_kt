package com.kuts.klaf.mnemonic

import android.app.Service
import android.content.Intent
import android.os.IBinder
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MnemonicGenerationForegroundService : Service(), KoinComponent {

    private val notifier: MnemonicGenerationNotifier by inject()

    override fun onCreate() {
        super.onCreate()
        startForeground(
            MnemonicGenerationNotifier.FOREGROUND_NOTIFICATION_ID,
            notifier.createProgressNotification(),
        )
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int = START_NOT_STICKY

    override fun onBind(intent: Intent?): IBinder? = null
}
