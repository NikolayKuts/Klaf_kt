package com.kuts.klaf.mnemonic

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.core.app.NotificationCompat
import com.kuts.domain.managers.MnemonicGenerationLaunchExtras
import com.kuts.domain.managers.MnemonicGenerationSource
import com.kuts.domain.managers.MnemonicGenerationType
import com.kuts.klaf.common.notifications.NotificationChannelInitializer.Companion.MNEMONIC_GENERATION_FAILURE_CHANNEL_ID
import com.kuts.klaf.common.notifications.NotificationChannelInitializer.Companion.MNEMONIC_GENERATION_PROGRESS_CHANNEL_ID
import com.kuts.klaf.common.notifications.NotificationChannelInitializer.Companion.MNEMONIC_GENERATION_SUCCESS_CHANNEL_ID
import com.kuts.klaf.data.R

class MnemonicGenerationNotifier(
    private val context: Context,
    private val notificationManager: NotificationManager,
    private val sessionIdProvider: () -> String = { "" },
) {

    companion object {

        const val FOREGROUND_NOTIFICATION_ID = 43_524
        private const val IMAGE_RESULT_NOTIFICATION_ID = 43_525
        private const val TEXT_RESULT_NOTIFICATION_ID = 43_526
        private const val ACTION_OPEN_RESULT = "com.kuts.klaf.action.OPEN_MNEMONIC_GENERATION_RESULT"
    }

    fun createProgressNotification(): Notification {
        return NotificationCompat.Builder(context, MNEMONIC_GENERATION_PROGRESS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_mnemonic_generation_24)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.mnemonic_generation_in_progress))
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setProgress(0, 0, true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    fun showSuccess(
        type: MnemonicGenerationType,
        source: MnemonicGenerationSource,
        clientSessionId: String = sessionIdProvider(),
    ) {
        notificationManager.notify(
            type.resultNotificationId(),
            NotificationCompat.Builder(context, MNEMONIC_GENERATION_SUCCESS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_mnemonic_image_ready_24)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(context.getString(type.successMessageResId()))
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setColor(Color.GREEN)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(createResultPendingIntent(type, source, clientSessionId))
                .setAutoCancel(true)
                .build(),
        )
    }

    fun showFailure(
        type: MnemonicGenerationType,
        source: MnemonicGenerationSource,
    ) {
        notificationManager.notify(
            type.resultNotificationId(),
            NotificationCompat.Builder(context, MNEMONIC_GENERATION_FAILURE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_mnemonic_image_failed_24)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(context.getString(type.failureMessageResId()))
                .setCategory(NotificationCompat.CATEGORY_ERROR)
                .setColor(Color.RED)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(createResultPendingIntent(type, source))
                .setAutoCancel(true)
                .build(),
        )
    }

    private fun createResultPendingIntent(
        type: MnemonicGenerationType,
        source: MnemonicGenerationSource,
        clientSessionId: String = sessionIdProvider(),
    ): PendingIntent? {
        val (launchDestination, deckId) = when (source) {
            is MnemonicGenerationSource.CardCreation -> {
                MnemonicGenerationLaunchExtras.DESTINATION_CARD_ADDITION to source.deckId
            }

            is MnemonicGenerationSource.CardEditing -> {
                MnemonicGenerationLaunchExtras.DESTINATION_CARD_EDITING to source.deckId
            }
        }
        val launchIntent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.apply {
                action = ACTION_OPEN_RESULT
                putExtra(com.kuts.domain.managers.ClientSessionLaunchExtras.SESSION_ID_KEY, clientSessionId)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(MnemonicGenerationLaunchExtras.DESTINATION_KEY, launchDestination)
                putExtra(MnemonicGenerationLaunchExtras.DECK_ID_KEY, deckId)

                if (source is MnemonicGenerationSource.CardEditing) {
                    putExtra(MnemonicGenerationLaunchExtras.CARD_ID_KEY, source.cardId)
                }
            }
            ?: return null

        return PendingIntent.getActivity(
            context,
            type.resultNotificationId(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun MnemonicGenerationType.resultNotificationId(): Int {
        return when (this) {
            MnemonicGenerationType.Text -> TEXT_RESULT_NOTIFICATION_ID
            MnemonicGenerationType.Image -> IMAGE_RESULT_NOTIFICATION_ID
        }
    }

    private fun MnemonicGenerationType.successMessageResId(): Int {
        return when (this) {
            MnemonicGenerationType.Text -> R.string.mnemonic_text_generation_succeeded
            MnemonicGenerationType.Image -> R.string.mnemonic_image_generation_succeeded
        }
    }

    private fun MnemonicGenerationType.failureMessageResId(): Int {
        return when (this) {
            MnemonicGenerationType.Text -> R.string.mnemonic_text_generation_failed
            MnemonicGenerationType.Image -> R.string.mnemonic_image_generation_failed
        }
    }
}
