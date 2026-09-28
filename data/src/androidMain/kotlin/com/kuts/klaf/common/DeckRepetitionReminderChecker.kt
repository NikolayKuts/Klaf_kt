package com.kuts.klaf.common

import android.content.Context
import androidx.work.*
import com.kuts.domain.common.getCurrentDateAsLong
import com.kuts.domain.common.ifTrue
import com.kuts.domain.entities.Deck
import com.kuts.domain.managers.IAccountScopedDeckReviewNotifier
import com.kuts.domain.repositories.ICrashlyticsRepository
import com.kuts.domain.useCases.FetchAllDecksUseCase
import com.kuts.klaf.room.databases.ActiveLocalRoomDatabase
import com.kuts.klaf.room.databases.AndroidSelectedAccountStore
import com.lib.lokdroid.core.logE
import java.util.concurrent.TimeUnit

class DeckRepetitionReminderChecker(
    context: Context,
    params: WorkerParameters,
    private val scopedNotifier: IAccountScopedDeckReviewNotifier,
    private val fetchAllDecks: FetchAllDecksUseCase,
    private val crashlytics: ICrashlyticsRepository,
    private val selectedAccount: AndroidSelectedAccountStore,
    private val activeLocalDatabase: ActiveLocalRoomDatabase,
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
        if (selectedAccount.areScopedRemindersActive()) {
            activeLocalDatabase.transaction {
                val scope = selectedAccount.currentScope()
                activeLocalDatabase.current().deckDao().getAllDecks().forEach { deck ->
                    if (deck.scheduledIterationDates.lastOrNull()?.let { it < getCurrentDateAsLong() } == true) {
                        scopedNotifier.showIfCurrent(scope, deck.name, deck.id)
                    }
                }
            }
        } else {
            fetchAllDecks().onEach { deck ->
                deck.shouldBeRepeated().ifTrue {
                    scopedNotifier.showIfCurrent(null, deck.name, deck.id)
                }
            }
        }
        Result.success()
    } catch (exception: Exception) {
        logE("DeckRepetitionReminderChecker failed\n${exception.stackTraceToString()}")
        crashlytics.report(exception = exception)
        Result.failure()
    }

    private fun Deck.shouldBeRepeated(): Boolean {
        return scheduledDate?.let { it < getCurrentDateAsLong() } ?: false
    }
}
