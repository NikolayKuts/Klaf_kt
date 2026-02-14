package com.kuts.klaf.common

import android.content.Context
import androidx.work.*
import com.kuts.domain.common.getCurrentDateAsLong
import com.kuts.domain.common.ifTrue
import com.kuts.domain.entities.Deck
import com.kuts.domain.managers.IDeckReviewNotifierManager
import com.kuts.domain.repositories.ICrashlyticsRepository
import com.kuts.domain.useCases.FetchAllDecksUseCase
import java.util.concurrent.TimeUnit

class DeckRepetitionReminderChecker(
    context: Context,
    params: WorkerParameters,
    private val deckReviewNotifier: IDeckReviewNotifierManager,
    private val fetchAllDecks: FetchAllDecksUseCase,
    private val crashlytics: ICrashlyticsRepository,
) : CoroutineWorker(appContext = context, params = params) {

    companion object {

        private const val UNIQUE_WORK_NAME = "deck_repetition_checking"
        private const val CHECKING_INTERVAL = 12L

        fun WorkManager.scheduleDeckRepetitionChecking() {
            this.enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<DeckRepetitionReminderChecker>(
                    CHECKING_INTERVAL,
                    TimeUnit.HOURS
                ).setInitialDelay(CHECKING_INTERVAL, TimeUnit.HOURS)
                    .build()
            )
        }
    }

    override suspend fun doWork(): Result = try {
        fetchAllDecks().onEach { deck ->
            deck.shouldBeRepeated().ifTrue {
                deckReviewNotifier.showNotification(deckName = deck.name, deckId = deck.id)
            }
        }
        Result.success()
    } catch (exception: Exception) {
        crashlytics.report(exception = exception)
        Result.failure()
    }

    private fun Deck.shouldBeRepeated(): Boolean {
        return scheduledDate?.let { it < getCurrentDateAsLong() } ?: false
    }
}
