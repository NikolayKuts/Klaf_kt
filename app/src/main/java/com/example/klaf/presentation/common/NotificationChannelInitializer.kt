package com.example.klaf.presentation.common

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import com.example.klaf.R
import com.example.klaf.presentation.deckRepetition.DeckRepetitionNotifier
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class NotificationChannelInitializer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deckRepetitionNotifier: DeckRepetitionNotifier,
    private val notificationManager: NotificationManager,
) {

    companion object {

        const val WORK_LOGIC_NOTIFICATION_CHANNEL_NAME = "Work logic channel"
        const val WORK_LOGIC_NOTIFICATION_CHANNEL_ID = "work_logic_channel_id"
    }

    fun initialize() {
        deckRepetitionNotifier.createDeckRepetitionNotificationChannel()
        createWorkLogicChannel()
    }

    private fun createWorkLogicChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_LOW

            val notificationChannel = NotificationChannel(
                WORK_LOGIC_NOTIFICATION_CHANNEL_ID,
                WORK_LOGIC_NOTIFICATION_CHANNEL_NAME,
                importance
            ).apply {
                description =
                    context.getString(R.string.work_logic_notification_channel_description)
                lightColor = Color.GREEN
                enableLights(true)
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(notificationChannel)
        }
    }
}