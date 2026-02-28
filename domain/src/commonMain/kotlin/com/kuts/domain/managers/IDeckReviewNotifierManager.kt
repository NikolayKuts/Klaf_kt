package com.kuts.domain.managers

interface IDeckReviewNotifierManager {

    fun showNotification(deckName: String, deckId: Int)

    fun showCommonNotification()

    fun removeNotificationFromNotificationBar(deckId: Int)
}
