package com.kuts.klaf.vocabularySource

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.core.app.NotificationCompat
import com.kuts.domain.managers.VocabularySourceAnalysisLaunchExtras
import com.kuts.klaf.common.notifications.NotificationChannelInitializer.Companion.VOCABULARY_SOURCE_ANALYSIS_FAILURE_CHANNEL_ID
import com.kuts.klaf.common.notifications.NotificationChannelInitializer.Companion.VOCABULARY_SOURCE_ANALYSIS_PROGRESS_CHANNEL_ID
import com.kuts.klaf.common.notifications.NotificationChannelInitializer.Companion.VOCABULARY_SOURCE_ANALYSIS_SUCCESS_CHANNEL_ID
import com.kuts.klaf.data.R

class VocabularySourceAnalysisNotifier(
    private val context: Context,
    private val notificationManager: NotificationManager,
    private val sessionIdProvider: () -> String = { "" },
) {

    companion object {

        const val FOREGROUND_NOTIFICATION_ID = 43_527
        private const val RESULT_NOTIFICATION_ID = 43_528
        private const val ACTION_OPEN_RESULT = "com.kuts.klaf.action.OPEN_VOCABULARY_SOURCE_ANALYSIS_RESULT"
    }

    fun createProgressNotification(): Notification =
        NotificationCompat.Builder(context, VOCABULARY_SOURCE_ANALYSIS_PROGRESS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_mnemonic_generation_24)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.vocabulary_source_analysis_in_progress))
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setProgress(0, 0, true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

    fun showSuccess(
        sourceId: Int,
        sourceTitle: String,
        clientSessionId: String = sessionIdProvider(),
    ) {
        val contentText = context.getString(
            R.string.vocabulary_source_analysis_succeeded,
            sourceTitle,
        )

        notificationManager.notify(
            RESULT_NOTIFICATION_ID,
            NotificationCompat.Builder(context, VOCABULARY_SOURCE_ANALYSIS_SUCCESS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_mnemonic_image_ready_24)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(contentText)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setColor(Color.GREEN)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(createResultPendingIntent(sourceId = sourceId, clientSessionId = clientSessionId))
                .setAutoCancel(true)
                .build(),
        )
    }

    fun showFailure(
        sourceId: Int,
        sourceTitle: String,
    ) {
        val contentText = context.getString(
            R.string.vocabulary_source_analysis_failed,
            sourceTitle,
        )

        notificationManager.notify(
            RESULT_NOTIFICATION_ID,
            NotificationCompat.Builder(context, VOCABULARY_SOURCE_ANALYSIS_FAILURE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_mnemonic_image_failed_24)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(contentText)
                .setCategory(NotificationCompat.CATEGORY_ERROR)
                .setColor(Color.RED)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(createResultPendingIntent(sourceId = sourceId))
                .setAutoCancel(true)
                .build(),
        )
    }

    private fun createResultPendingIntent(sourceId: Int, clientSessionId: String = sessionIdProvider()): PendingIntent? {
        val launchIntent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.apply {
                action = ACTION_OPEN_RESULT
                putExtra(com.kuts.domain.managers.ClientSessionLaunchExtras.SESSION_ID_KEY, clientSessionId)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(VocabularySourceAnalysisLaunchExtras.SOURCE_ID_KEY, sourceId)
            }
            ?: return null

        return PendingIntent.getActivity(
            context,
            RESULT_NOTIFICATION_ID,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
