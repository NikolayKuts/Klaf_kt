package com.kuts.klaf.common

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.kuts.domain.useCases.FetchAllDecksUseCase
import com.lib.lokdroid.core.logD

class DeckReviewRescheduler(
    private val appContext: Context,
    private val parameters: WorkerParameters,
    private var fetchAllDecksUseCase: FetchAllDecksUseCase,
    private var deckReviewingReminder: IDeckReviewScheduler,
) : CoroutineWorker(
    appContext = appContext,
    params = parameters
) {

    companion object {

        private const val UNIQUE_WORK_NAME = "deck_review_rescheduling_work"

        fun WorkManager.launchDeckReviewRescheduling(): Operation = enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            makeWorkRequest()
        )

        private fun makeWorkRequest(): OneTimeWorkRequest {
            return OneTimeWorkRequestBuilder<DeckReviewRescheduler>().build()
        }
    }

    override suspend fun doWork(): Result = try {
        logD("doWork() called")

        val decks = fetchAllDecksUseCase.invoke()

        decks.forEach { deck ->
            deckReviewingReminder.schedule(
                deckName = deck.name,
                deckId = deck.id,
                atTime = deck.scheduledDate ?: return@forEach
            )
        }

        Result.success()
    } catch (e: Exception) {
        Result.failure()
    }
}
