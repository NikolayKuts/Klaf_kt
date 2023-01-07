package com.example.klaf.data.common

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import com.example.klaf.R
import com.example.klaf.presentation.common.NotificationChannelInitializer.Companion.WORK_LOGIC_NOTIFICATION_CHANNEL_ID
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AppRestartNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun createAppRestartNotification(): Notification {
        return NotificationCompat.Builder(context, WORK_LOGIC_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_restart_24)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.app_restart_notification_template))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}