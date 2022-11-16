package com.example.klaf.presentation.common

import com.example.klaf.presentation.deckList.dataSynchronization.DataSynchronizationNotifier
import com.example.klaf.presentation.deckRepetition.DeckRepetitionNotifier
import javax.inject.Inject

class NotificationChannelInitializer @Inject constructor(
    private val deckRepetitionNotifier: DeckRepetitionNotifier,
    private val dataSynchronizationNotifier: DataSynchronizationNotifier,
) {

    fun initialize() {
        deckRepetitionNotifier.createDeckRepetitionNotificationChannel()
        dataSynchronizationNotifier.createSynchronizationNotificationChannel()
    }
}