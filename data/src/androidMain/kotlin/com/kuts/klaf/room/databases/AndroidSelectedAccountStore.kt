package com.kuts.klaf.room.databases

import android.content.Context
import com.kuts.domain.managers.GUEST_REVIEW_REMINDER_SCOPE
import com.kuts.domain.managers.IReviewReminderScopeProvider

private const val PREFERENCES_NAME = "klaf_local_account_selection"
private const val SELECTED_ACCOUNT_KEY = "selected_account_email"
private const val SCOPED_REMINDERS_ACTIVE_KEY = "scoped_reminders_active"

class AndroidSelectedAccountStore(context: Context) : SelectedAccountStore, IReviewReminderScopeProvider {

    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun read(): String? = preferences.getString(SELECTED_ACCOUNT_KEY, null)

    override fun write(email: String?) {
        val saved = preferences.edit()
            .putString(SELECTED_ACCOUNT_KEY, email)
            .putBoolean(SCOPED_REMINDERS_ACTIVE_KEY, true)
            .commit()
        check(saved) {
            "Unable to persist selected account"
        }
    }

    fun areScopedRemindersActive(): Boolean = preferences.getBoolean(SCOPED_REMINDERS_ACTIVE_KEY, false)

    override fun currentScope(): String? = if (areScopedRemindersActive()) read() ?: GUEST_REVIEW_REMINDER_SCOPE else null
}
