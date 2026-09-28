package com.kuts.klaf.room.databases

interface ScopedDeckReminderActions {

    fun cancel(accountEmail: String?, deckId: Int)

    fun dismissNotification(accountEmail: String?, deckId: Int)

    fun schedule(accountEmail: String?, deckName: String, deckId: Int, atTime: Long)
}

class RoomReminderSelectionObserver(
    private val reminders: ScopedDeckReminderActions,
) : LocalRoomSelectionObserver {

    override suspend fun onSelectionChanged(previous: LocalRoomSelection, current: LocalRoomSelection) {
        previous.database.deckDao().getAllDecks().forEach { deck ->
            reminders.cancel(previous.accountEmail, deck.id)
            reminders.dismissNotification(previous.accountEmail, deck.id)
        }
        current.database.deckDao().getAllDecks().forEach { deck ->
            val scheduledDate = deck.scheduledIterationDates.lastOrNull() ?: return@forEach
            reminders.schedule(current.accountEmail, deck.name, deck.id, scheduledDate)
        }
    }
}
