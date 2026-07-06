package com.kuts.klaf.common

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kuts.domain.common.UNASSIGNED_INT_VALUE
import com.kuts.domain.managers.IDeckReviewNotifierManager
import com.kuts.domain.managers.IDeckReviewScheduler
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

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

class AndroidDeckReviewingReminder(
    private val context: Context,
) : IDeckReviewScheduler {

    override fun schedule(
        deckName: String,
        deckId: Int,
        atTime: Long,
    ) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)

        val intent = Intent(context, DeckReviewReceiver::class.java)
            .prepare(deckName = deckName, deckId = deckId)

        val pendingIntent = intent.toPendingIntent(deckId = deckId)

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            atTime,
            pendingIntent
        )
    }

    override fun cancel(deckId: Int) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, DeckReviewReceiver::class.java).apply {
            action = ACTION
        }
        val pendingIntent = intent.toPendingIntent(deckId = deckId)

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun Intent.prepare(
        deckName: String,
        deckId: Int,
    ): Intent = apply {
        action = ACTION
        putExtra(DECK_ID_EXTRA_KEY, deckId)
        putExtra(DECK_NAME_EXTRA_KEY, deckName)
    }

    private fun Intent.toPendingIntent(deckId: Int): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            deckId,
            this,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

class DeckReviewReceiver : BroadcastReceiver(), KoinComponent {

    private val deckReviewNotifier: IDeckReviewNotifierManager by inject()

    override fun onReceive(context: Context, intent: Intent) {
        executeIfIntentValid(intent = intent) { deckId, deckName ->
            deckReviewNotifier.showNotification(
                deckName = deckName,
                deckId = deckId
            )
        }
    }
}
