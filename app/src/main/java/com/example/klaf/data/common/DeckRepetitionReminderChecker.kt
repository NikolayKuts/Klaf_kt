package com.example.klaf.data.common

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.domain.common.getCurrentDateAsLong
import com.example.domain.common.ifTrue
import com.example.domain.entities.Deck
import com.example.domain.useCases.FetchAllDecksUseCase
import com.example.klaf.data.common.notifications.DeckRepetitionNotifier
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class DeckRepetitionReminderChecker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val deckRepetitionNotifier: DeckRepetitionNotifier,
    private val fetchAllDecks: FetchAllDecksUseCase,
) : CoroutineWorker(appContext = context, params = params) {

    companion object {

        private const val UNIQUE_WORK_NAME = "deck_repetition_checking"
        private const val CHECKING_INTERVAL = 1L

        fun WorkManager.scheduleDeckRepetitionChecking() {
            this.enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<DeckRepetitionReminderChecker>(
                    CHECKING_INTERVAL,
                    TimeUnit.DAYS
                ).build()
            )
        }
    }

    override suspend fun doWork(): Result {
        fetchAllDecks().any { deck -> deck.shouldBeRepeated() }
            .ifTrue { deckRepetitionNotifier.showCommonNotification() }

        return Result.success()
    }

    private fun Deck.shouldBeRepeated(): Boolean {
        return scheduledDate?.let { it < getCurrentDateAsLong() } ?: false
    }
}