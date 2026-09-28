package com.kuts.domain.managers

const val GUEST_REVIEW_REMINDER_SCOPE = "@guest"

interface IReviewReminderScopeProvider {

    /** Null means the legacy, unscoped reminder mode is still in use. */
    fun currentScope(): String?
}

interface IAccountScopedDeckReviewNotifier {

    fun showIfCurrent(accountScope: String?, deckName: String, deckId: Int)
}
