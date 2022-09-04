package com.example.klaf.presentation.deckList.dataSynchronization

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.klaf.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class DataSynchronizationNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationManager: NotificationManager
) {

    companion object {

        private const val CHANNEL_ID = "synchronization_channel_id"
        private const val CHANNEL_NAME = "synchronization_channel_name"
    }

    fun createSynchronizationNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.data_synchronization_channel_description)
                notificationManager.createNotificationChannel(this)
            }
        }
    }

    fun createNotification(progress: Int): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_sync_24)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.data_synchronization_notifier_content_text))
            .setProgress(100, progress, false)
            .setOnlyAlertOnce(true)
            .build()
    }
}