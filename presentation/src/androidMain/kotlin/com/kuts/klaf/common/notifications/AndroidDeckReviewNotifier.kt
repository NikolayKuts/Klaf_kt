package com.kuts.klaf.common.notifications

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.kuts.domain.managers.IDeckReviewNotifierManager
import com.kuts.klaf.navigation.AppLaunchNavigationExtras
import com.kuts.klaf.presentation.R

class AndroidDeckReviewNotifier(
    private val context: Context,
    private val notificationManager: NotificationManager
) : IDeckReviewNotifierManager {

    companion object Companion {

        private const val DECK_REPETITION_GROUP_KEY = "deck_repetition_group"
        private const val COMMON_NOTIFICATION_ID = 435243543
        private const val MIN_NOTIFICATION_FOR_GROUP = 4
        private const val DECK_REPETITION_CHANNEL_ID = "deck_repetition_channel_id"
        private const val MAIN_ACTIVITY_CLASS_NAME = "com.kuts.klaf.MainActivity"
    }

    override fun showNotification(deckName: String, deckId: Int) {
        val notification = createDeckRepetitionNotification(deckName = deckName, deckId = deckId)

        notificationManager.notify(deckId, notification)
        showSummeryNotificationIfSdkLessThan24AndMoreThan22()
    }

    override fun showCommonNotification() {
        notificationManager.notify(COMMON_NOTIFICATION_ID, createCommonDeckRepetitionNotification())
    }

    override fun removeNotificationFromNotificationBar(deckId: Int) {
        notificationManager.cancel(deckId)
    }

    private fun showSummeryNotificationIfSdkLessThan24AndMoreThan22() {
        if (
            Build.VERSION.SDK_INT < Build.VERSION_CODES.N
            && Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1
        ) {
            if (notificationManager.activeNotifications.size >= MIN_NOTIFICATION_FOR_GROUP) {
                val summeryNotification =
                    createSummeryNotification(notificationManager.activeNotifications.size)
                notificationManager.notify(COMMON_NOTIFICATION_ID, summeryNotification)
            }
        }
    }

    private fun createDeckRepetitionNotification(deckName: String, deckId: Int): Notification {
        return NotificationCompat.Builder(context, DECK_REPETITION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_deck_repetition_notification_24)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(
                context.getString(R.string.deck_repetition_notification_template, deckName)
            )
            .setGroupIfSdkLessThan24(groupKey = DECK_REPETITION_GROUP_KEY)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(createDeckRepetitionPendingIntent(deckId = deckId, deckName = deckName))
            .build()
    }

    private fun createSummeryNotification(notificationQuantity: Int): Notification {
        return NotificationCompat.Builder(context, DECK_REPETITION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_deck_repetition_notification_24)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setGroup(DECK_REPETITION_GROUP_KEY)
            .setGroupSummary(true)
            .setContentText(
                context.getString(R.string.deck_repetition_summery_notification_content_text)
            )
            .setSummeryStyle(notificationQuantity = notificationQuantity)
            .setContentIntent(createDeckListPendingIntent())
            .build()
    }

    private fun createCommonDeckRepetitionNotification(): Notification {
        return NotificationCompat.Builder(context, DECK_REPETITION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_deck_repetition_notification_24)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(
                context.getString(R.string.deck_repetition_common_notification_template)
            )
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(createDeckListPendingIntent())
            .build()
    }

    private fun NotificationCompat.Builder.setSummeryStyle(
        notificationQuantity: Int,
    ): NotificationCompat.Builder {
        return this.setStyle(
            NotificationCompat.InboxStyle().setBigContentTitle(context.getString(R.string.app_name))
                .addLine(
                    context.getString(
                        R.string.deck_repetition_summery_notification_big_content_title,
                        notificationQuantity
                    )
                )
                .setSummaryText(
                    context.getString(
                        R.string.deck_repetition_summery_notification_summery_text,
                        notificationQuantity
                    )
                )
        )
    }

    private fun NotificationCompat.Builder.setGroupIfSdkLessThan24(
        groupKey: String,
    ): NotificationCompat.Builder {
        return this.apply {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                this.setGroup(groupKey)
            }
        }
    }

    private fun createDeckRepetitionPendingIntent(
        deckId: Int,
        deckName: String
    ): PendingIntent {
        val intent = createBaseMainActivityIntent().apply {
            putExtra(
                AppLaunchNavigationExtras.DESTINATION_KEY,
                AppLaunchNavigationExtras.DESTINATION_DECK_REPETITION,
            )
            putExtra(AppLaunchNavigationExtras.DECK_ID_KEY, deckId)
            putExtra(AppLaunchNavigationExtras.DECK_NAME_KEY, deckName)
        }

        return createPendingIntent(requestCode = deckId, intent = intent)
    }

    private fun createDeckListPendingIntent(): PendingIntent {
        val intent = createBaseMainActivityIntent().apply {
            putExtra(
                AppLaunchNavigationExtras.DESTINATION_KEY,
                AppLaunchNavigationExtras.DESTINATION_DECK_LIST,
            )
        }

        return createPendingIntent(requestCode = COMMON_NOTIFICATION_ID, intent = intent)
    }

    private fun createBaseMainActivityIntent(): Intent {
        return Intent().apply {
            setClassName(context, MAIN_ACTIVITY_CLASS_NAME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
    }

    private fun createPendingIntent(requestCode: Int, intent: Intent): PendingIntent {
        val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        return PendingIntent.getActivity(context, requestCode, intent, pendingIntentFlags)
    }
}
