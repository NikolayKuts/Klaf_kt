package com.kuts.klaf.data.common.notifications

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import com.kuts.klaf.R
import com.kuts.klaf.data.common.notifications.NotificationChannelInitializer.Companion.WORK_LOGIC_NOTIFICATION_CHANNEL_ID
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class DataSynchronizationNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    companion object {

        private const val MAX_PROGRESS_VALUE = 100
    }

    fun createNotification(progress: Int? = null): Notification {
        return NotificationCompat.Builder(context, WORK_LOGIC_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_sync_24)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.data_synchronization_notifier_content_text))
            .ifNotNullSetProgress(progress = progress)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun NotificationCompat.Builder.ifNotNullSetProgress(
        progress: Int?,
    ): NotificationCompat.Builder {
        progress?.let { notNullableProgress ->
            setProgress(MAX_PROGRESS_VALUE, notNullableProgress, false)
        }
        return this
    }
}