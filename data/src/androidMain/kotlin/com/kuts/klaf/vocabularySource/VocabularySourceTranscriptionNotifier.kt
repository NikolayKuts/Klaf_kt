package com.kuts.klaf.vocabularySource

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.core.app.NotificationCompat
import com.kuts.domain.managers.VocabularySourceAnalysisLaunchExtras
import com.kuts.klaf.common.notifications.NotificationChannelInitializer.Companion.VOCABULARY_SOURCE_TRANSCRIPTION_FAILURE_CHANNEL_ID
import com.kuts.klaf.common.notifications.NotificationChannelInitializer.Companion.VOCABULARY_SOURCE_TRANSCRIPTION_PROGRESS_CHANNEL_ID
import com.kuts.klaf.common.notifications.NotificationChannelInitializer.Companion.VOCABULARY_SOURCE_TRANSCRIPTION_SUCCESS_CHANNEL_ID
import com.kuts.klaf.data.R

class VocabularySourceTranscriptionNotifier(
    private val context: Context,
    private val notificationManager: NotificationManager,
) {

    companion object {

        const val FOREGROUND_NOTIFICATION_ID = 43_530
        private const val RESULT_NOTIFICATION_ID = 43_531
        private const val ACTION_OPEN_RESULT = "com.kuts.klaf.action.OPEN_VOCABULARY_SOURCE_ANALYSIS_RESULT"
    }

    fun createProgressNotification(): Notification =
        NotificationCompat.Builder(context, VOCABULARY_SOURCE_TRANSCRIPTION_PROGRESS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_mnemonic_generation_24)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.vocabulary_source_transcription_in_progress))
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setProgress(0, 0, true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

    fun showSuccess(
        sourceId: Int,
        sourceTitle: String,
    ) {
        val contentText = context.getString(
            R.string.vocabulary_source_transcription_succeeded,
            sourceTitle,
        )

        notificationManager.notify(
            RESULT_NOTIFICATION_ID,
            NotificationCompat.Builder(context, VOCABULARY_SOURCE_TRANSCRIPTION_SUCCESS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_mnemonic_image_ready_24)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(contentText)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setColor(Color.GREEN)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(createResultPendingIntent(sourceId = sourceId))
                .setAutoCancel(true)
                .build(),
        )
    }

    fun showFailure(
        sourceId: Int,
        sourceTitle: String,
    ) {
        val contentText = context.getString(
            R.string.vocabulary_source_transcription_failed,
            sourceTitle,
        )

        notificationManager.notify(
            RESULT_NOTIFICATION_ID,
            NotificationCompat.Builder(context, VOCABULARY_SOURCE_TRANSCRIPTION_FAILURE_CHANNEL_ID)
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

    private fun createResultPendingIntent(sourceId: Int): PendingIntent? {
        val launchIntent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.apply {
                action = ACTION_OPEN_RESULT
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
