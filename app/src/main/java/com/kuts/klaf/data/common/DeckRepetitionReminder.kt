package com.kuts.klaf.data.common

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.kuts.klaf.data.common.notifications.DeckRepetitionNotifier
import com.kuts.domain.common.UNASSIGNED_INT_VALUE
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class DeckRepetitionReminder @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted private val parameters: WorkerParameters,
    private val deckRepetitionNotifier: DeckRepetitionNotifier,
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
            cancelUniqueWork(deckId.toString())
            enqueueUniqueWork(
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
            val delayDuration = atTime - currentTime

            return OneTimeWorkRequestBuilder<DeckRepetitionReminder>()
                .setInitialDelay(delayDuration, TimeUnit.MILLISECONDS)
                .setInputData(workData)
                .build()
        }
    }

    override suspend fun doWork(): Result {
        deckRepetitionNotifier.showNotification(
            deckName = parameters.inputData.getString(DECK_NAME) ?: "",
            deckId = parameters.inputData.getInt(DECK_ID, UNASSIGNED_INT_VALUE)
        )

        return Result.success()
    }
}