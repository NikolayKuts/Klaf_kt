package com.example.klaf.data

import android.content.Context
import androidx.work.*
import com.example.klaf.domain.auxiliary.DateAssistant
import com.example.klaf.presentation.common.Notifier
import com.example.klaf.presentation.common.log
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

        private const val UNIQUE_WORK_NAME = "repetition_scheduling"

        fun WorkManager.scheduleDeckRepetition(
            deckName: String,
            deckId: Int,
            scheduledTime: Long = 0,
        ) {
            this.enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                makeWorkRequest(deckName = deckName, deckId = deckId, scheduleTime = scheduledTime)
            )
        }

        private fun makeWorkRequest(
            deckName: String,
            deckId: Int,
            scheduleTime: Long,
        ): OneTimeWorkRequest {
            val workData = workDataOf(
                DECK_NAME to deckName,
                DECK_ID to deckId
            )

            val currentTime = System.currentTimeMillis()
            log(message = (DateAssistant.getFormattedDate(scheduleTime)))

            return OneTimeWorkRequestBuilder<DeckRepetitionReminder>()
                .setInitialDelay(scheduleTime - currentTime, TimeUnit.MILLISECONDS)
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