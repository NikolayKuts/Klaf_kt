package com.kuts.klaf.common

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kuts.domain.common.UNASSIGNED_INT_VALUE
import com.kuts.domain.managers.IDeckReviewNotifierManager
import com.kuts.domain.managers.IDeckReviewScheduler as DomainDeckReviewScheduler
import com.lib.lokdroid.core.logD
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DeckReviewingReminder(
    private val context: Context,
) : IDeckReviewScheduler, DomainDeckReviewScheduler {

    companion object {

        private const val ACTION = "deck_review_scheduling"
        private const val DECK_ID_EXTRA_KEY = "deckId"
        private const val DECK_NAME_EXTRA_KEY = "deckName"

        private fun Intent.retrieveDeckIdAndName(): Pair<Int, String?> {
            val deckId = getIntExtra(DECK_ID_EXTRA_KEY, UNASSIGNED_INT_VALUE)
            val deckName = getStringExtra(DECK_NAME_EXTRA_KEY)

            return deckId to deckName
        }

        private fun executeIfIntentValid(
            intent: Intent,
            block: (deckId: Int, deckName: String) -> Unit
        ) {
            if (intent.action == ACTION) {
                val (deckId, deckName) = intent.retrieveDeckIdAndName()

                if (deckId == UNASSIGNED_INT_VALUE || deckName == null) return

                block(deckId, deckName)
            }
        }
    }

    override fun schedule(
        deckName: String,
        deckId: Int,
        atTime: Long,
    ) {
        logD("DeckReviewingReminder.schedule() called. Deck id: $deckId, name: $deckName")

        val alarmManager = context.getSystemService(AlarmManager::class.java)

        val intent = Intent(context, DeckReviewReceiver::class.java)
            .prepare(deckName = deckName, deckId = deckId)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            deckId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            atTime,
            pendingIntent
        )
    }

    private fun Intent.prepare(
        deckName: String,
        deckId: Int,
    ): Intent = apply {
        action = ACTION
        putExtra(DECK_ID_EXTRA_KEY, deckId)
        putExtra(DECK_NAME_EXTRA_KEY, deckName)
    }

    class DeckReviewReceiver : BroadcastReceiver(), KoinComponent {

        private val deckReviewNotifier: IDeckReviewNotifierManager by inject()

        override fun onReceive(context: Context, intent: Intent) {
            executeIfIntentValid(intent = intent) { deckId, deckName ->
                logD("onReceive() called. Deck id: $deckId, name: $deckName")

                deckReviewNotifier.showNotification(
                    deckName = deckName,
                    deckId = deckId
                )
            }
        }
    }
}
