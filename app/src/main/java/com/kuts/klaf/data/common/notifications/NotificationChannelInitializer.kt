package com.kuts.klaf.data.common.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import com.kuts.klaf.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class NotificationChannelInitializer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationManager: NotificationManager,
) {

    companion object {

        const val WORK_LOGIC_NOTIFICATION_CHANNEL_NAME = "Work logic channel"
        const val WORK_LOGIC_NOTIFICATION_CHANNEL_ID = "work_logic_channel_id"
        const val DECK_REPETITION_CHANNEL_NAME = "Deck repetition channel"
        const val DECK_REPETITION_CHANNEL_ID = "deck_repetition_channel_id"
    }

    fun initialize() {
        createDeckRepetitionChannel()
        createWorkLogicChannel()
    }

    private fun createWorkLogicChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                WORK_LOGIC_NOTIFICATION_CHANNEL_ID,
                WORK_LOGIC_NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
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

    private fun createDeckRepetitionChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                DECK_REPETITION_CHANNEL_ID,
                DECK_REPETITION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.deck_repetition_channel_description)
                lightColor = Color.GREEN
                enableLights(true)
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(notificationChannel)
        }
    }
}