package com.kuts.klaf.data.common

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.kuts.domain.common.getCurrentDateAsLong
import com.kuts.domain.common.ifTrue
import com.kuts.domain.entities.Deck
import com.kuts.domain.repositories.CrashlyticsRepository
import com.kuts.domain.useCases.FetchAllDecksUseCase
import com.kuts.klaf.data.common.notifications.DeckRepetitionNotifier
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class DeckRepetitionReminderChecker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val deckRepetitionNotifier: DeckRepetitionNotifier,
    private val fetchAllDecks: FetchAllDecksUseCase,
    private val crashlytics: CrashlyticsRepository,
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
                deckRepetitionNotifier.showNotification(deckName = deck.name, deckId = deck.id)
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