package com.kuts.klaf.common

import androidx.work.WorkManager
import com.kuts.klaf.common.DeckReviewRescheduler.Companion.launchDeckReviewRescheduling
import com.kuts.klaf.room.databases.AndroidSelectedAccountStore

/** Rebuilds scoped alarms after a process stops between account selection and alarm scheduling. */
class AndroidScopedReminderStartupRecovery(
    selectedAccount: AndroidSelectedAccountStore,
    workManager: WorkManager,
) {

    init {
        if (selectedAccount.areScopedRemindersActive()) {
            workManager.launchDeckReviewRescheduling()
        }
    }
}
