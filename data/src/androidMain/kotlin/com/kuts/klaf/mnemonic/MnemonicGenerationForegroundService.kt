package com.kuts.klaf.mnemonic

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.lib.lokdroid.core.logD
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MnemonicGenerationForegroundService : Service(), KoinComponent {

    private val notifier: MnemonicGenerationNotifier by inject()
    private val diagnostics: AndroidMnemonicGenerationDiagnostics by inject()
    private var generationWakeLock: AndroidMnemonicGenerationWakeLock? = null
    private var serviceInstanceId = 0L

    override fun onCreate() {
        super.onCreate()
        serviceInstanceId = diagnostics.foregroundServiceCreating()
        logD("Mnemonic foreground service creating: ${diagnostics.snapshot()}")
        startForeground(
            MnemonicGenerationNotifier.FOREGROUND_NOTIFICATION_ID,
            notifier.createProgressNotification(),
        )
        generationWakeLock = AndroidMnemonicGenerationWakeLock(context = this).also { it.acquire() }
        diagnostics.foregroundServiceEnteredForeground(serviceInstanceId)
        logD(
            "Mnemonic foreground service entered foreground: " +
                "wakeLockHeld=${generationWakeLock?.isHeld == true}; ${diagnostics.snapshot()}",
        )
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        diagnostics.foregroundServiceStarted(serviceInstanceId)
        logD(
            "Mnemonic foreground service started: startId=$startId, flags=$flags, " +
                "action=${intent?.action}; ${diagnostics.snapshot()}",
        )
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        logD("Mnemonic foreground service task removed: ${diagnostics.snapshot()}")
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        try {
            diagnostics.foregroundServiceDestroying(serviceInstanceId)
            logD("Mnemonic foreground service destroying: ${diagnostics.snapshot()}")
        } finally {
            val wakeLock = generationWakeLock
            val wasWakeLockHeld = wakeLock?.isHeld == true
            generationWakeLock = null
            try {
                wakeLock?.release()
                logD("Mnemonic foreground service wake lock released: wasHeld=$wasWakeLockHeld")
            } finally {
                super.onDestroy()
                diagnostics.foregroundServiceDestroyed(serviceInstanceId)
                logD("Mnemonic foreground service destroyed: ${diagnostics.snapshot()}")
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
