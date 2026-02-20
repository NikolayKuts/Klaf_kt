package com.kuts.klaf.common.notifications

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import com.kuts.klaf.data.R
import com.kuts.klaf.common.notifications.NotificationChannelInitializer.Companion.WORK_LOGIC_NOTIFICATION_CHANNEL_ID
class AppRestartNotifier(
    private val context: Context,
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
