package com.kuts.klaf.vocabularySource

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.lib.lokdroid.core.logD
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class VocabularySourceTranscriptionForegroundService : Service(), KoinComponent {

    private val notifier: VocabularySourceTranscriptionNotifier by inject()

    override fun onCreate() {
        super.onCreate()
        logD("Vocabulary Source transcription foreground service creating.")
        startForeground(
            VocabularySourceTranscriptionNotifier.FOREGROUND_NOTIFICATION_ID,
            notifier.createProgressNotification(),
        )
        logD("Vocabulary Source transcription foreground service entered foreground.")
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        logD(
            "Vocabulary Source transcription foreground service started: " +
                "startId=$startId, flags=$flags, action=${intent?.action}",
        )
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        logD("Vocabulary Source transcription foreground service task removed.")
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        logD("Vocabulary Source transcription foreground service destroying.")
        super.onDestroy()
        logD("Vocabulary Source transcription foreground service destroyed.")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
