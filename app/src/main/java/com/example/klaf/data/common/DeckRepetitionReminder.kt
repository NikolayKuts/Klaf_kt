package com.example.klaf.data.common

import android.content.Context
import androidx.work.*
import com.example.klaf.presentation.common.Notifier
import java.util.concurrent.TimeUnit

class DeckRepetitionReminder(
    private val appContext: Context,
    private val parameters: WorkerParameters,
) : CoroutineWorker(
    appContext = appContext,
    params = parameters
) {

    companion object {

        private const val DECK_NAME = "deck_name"
        private const val DECK_ID = "deck_id"

        fun WorkManager.scheduleDeckRepetition(
            deckName: String,
            deckId: Int,
            atTime: Long = 0,
        ) {
            this.enqueueUniqueWork(
                deckId.toString(),
                ExistingWorkPolicy.KEEP,
                makeWorkRequest(deckName = deckName, deckId = deckId, atTime = atTime)
            )
        }

        private fun makeWorkRequest(
            deckName: String,
            deckId: Int,
            atTime: Long,
        ): OneTimeWorkRequest {
            val workData = workDataOf(
                DECK_NAME to deckName,
                DECK_ID to deckId
            )

            val currentTime = System.currentTimeMillis()

            return OneTimeWorkRequestBuilder<DeckRepetitionReminder>()
                .setInitialDelay(atTime - currentTime, TimeUnit.MILLISECONDS)
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