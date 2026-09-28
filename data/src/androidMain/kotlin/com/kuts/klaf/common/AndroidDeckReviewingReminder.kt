package com.kuts.klaf.common

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.kuts.domain.common.UNASSIGNED_INT_VALUE
import com.kuts.domain.managers.GUEST_REVIEW_REMINDER_SCOPE
import com.kuts.domain.managers.IAccountScopedDeckReviewNotifier
import com.kuts.domain.managers.IDeckReviewScheduler
import com.kuts.klaf.room.databases.AndroidSelectedAccountStore
import com.kuts.klaf.room.databases.ScopedDeckReminderActions
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private const val ACTION = "deck_review_scheduling"
private const val DECK_ID_EXTRA_KEY = "deckId"
private const val DECK_NAME_EXTRA_KEY = "deckName"
private const val ACCOUNT_SCOPE_EXTRA_KEY = "reminderAccountScope"

private fun accountScope(email: String?): String = email ?: GUEST_REVIEW_REMINDER_SCOPE

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

    private val selectedAccount = AndroidSelectedAccountStore(context)
    private val scopedActions = AndroidScopedDeckReminderActions(context)

    override fun schedule(
        deckName: String,
        deckId: Int,
        atTime: Long,
    ) {
        if (selectedAccount.areScopedRemindersActive()) {
            scopedActions.schedule(selectedAccount.read(), deckName, deckId, atTime)
            return
        }
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
        if (selectedAccount.areScopedRemindersActive()) {
            scopedActions.cancel(selectedAccount.read(), deckId)
            return
        }
        cancelLegacy(deckId)
    }

    internal fun cancelLegacy(deckId: Int) {
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

class AndroidScopedDeckReminderActions(
    private val context: Context,
) : ScopedDeckReminderActions {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    override fun cancel(accountEmail: String?, deckId: Int) {
        val pendingIntent = scopedPendingIntent(accountEmail, deckId, PendingIntent.FLAG_NO_CREATE)
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
        cancelLegacy(deckId)
    }

    override fun dismissNotification(accountEmail: String?, deckId: Int) {
        notificationManager.cancel(accountScope(accountEmail), deckId)
        notificationManager.cancel(deckId)
    }

    override fun schedule(accountEmail: String?, deckName: String, deckId: Int, atTime: Long) {
        val pendingIntent = requireNotNull(
            scopedPendingIntent(accountEmail, deckId, PendingIntent.FLAG_UPDATE_CURRENT, deckName),
        )
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atTime, pendingIntent)
    }

    private fun cancelLegacy(deckId: Int) {
        val intent = Intent(context, DeckReviewReceiver::class.java).apply { action = ACTION }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            deckId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun scopedPendingIntent(
        accountEmail: String?,
        deckId: Int,
        flag: Int,
        deckName: String? = null,
    ): PendingIntent? {
        val scope = accountScope(accountEmail)
        val intent = Intent(context, DeckReviewReceiver::class.java).apply {
            action = ACTION
            data = Uri.parse("klaf://review/${Uri.encode(scope)}/$deckId")
            putExtra(ACCOUNT_SCOPE_EXTRA_KEY, scope)
            putExtra(DECK_ID_EXTRA_KEY, deckId)
            if (deckName != null) putExtra(DECK_NAME_EXTRA_KEY, deckName)
        }
        return PendingIntent.getBroadcast(context, deckId, intent, flag or PendingIntent.FLAG_IMMUTABLE)
    }
}

class DeckReviewReceiver : BroadcastReceiver(), KoinComponent {

    private val scopedNotifier: IAccountScopedDeckReviewNotifier by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val selectedAccount = AndroidSelectedAccountStore(context)
        val scope = intent.getStringExtra(ACCOUNT_SCOPE_EXTRA_KEY)
        if (scope == null && selectedAccount.areScopedRemindersActive()) return
        if (scope != null && scope != accountScope(selectedAccount.read())) return
        executeIfIntentValid(intent = intent) { deckId, deckName ->
            scopedNotifier.showIfCurrent(scope, deckName, deckId)
        }
    }
}
