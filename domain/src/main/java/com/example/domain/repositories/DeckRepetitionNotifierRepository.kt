package com.example.domain.repositories

interface DeckRepetitionNotifierRepository {

    fun showNotification(deckName: String, deckId: Int)

    fun removeNotificationFromNotificationBar(deckId: Int)
}