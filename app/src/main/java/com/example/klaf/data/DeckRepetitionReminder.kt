package com.example.klaf.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.example.klaf.R
import com.example.klaf.presentation.common.Notifier
import com.example.klaf.presentation.common.log
import kotlinx.coroutines.delay

class DeckRepetitionReminder(
    private val appContext: Context,
    private val parameters: WorkerParameters,
) : CoroutineWorker(
    appContext = appContext,
    params = parameters
) {

    companion object {

        private const val CHANNEL_NAME = "channel_name"
        private const val DECK_NAME = "deck_name"
        private const val DECK_ID = "deck_id"

        private const val UNIQUE_WORK_NAME = "repetition_scheduling"


        fun WorkManager.scheduleDeckRepetition(deckName: String, deckId: Int) {
            this.enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                makeWorkRequest(deckName = deckName, deckId = deckId)
            )
        }

        private fun makeWorkRequest(deckName: String, deckId: Int): OneTimeWorkRequest {
            val workData = workDataOf(
                DECK_NAME to deckName,
                DECK_ID to deckId
            )

            return OneTimeWorkRequestBuilder<DeckRepetitionReminder>()
                .setInputData(workData)
                .build()
        }
    }

    override suspend fun doWork(): Result {
        Notifier(context = appContext).showNotification(
            deckName = parameters.inputData.getString(DECK_NAME) ?: "",
            deckId = parameters.inputData.getInt(DECK_ID, -1)
        )

        return Result.success()
    }






}