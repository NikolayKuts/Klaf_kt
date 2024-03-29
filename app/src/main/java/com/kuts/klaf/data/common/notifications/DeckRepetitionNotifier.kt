package com.kuts.klaf.data.common.notifications

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.os.bundleOf
import androidx.navigation.NavDeepLinkBuilder
import com.kuts.klaf.R
import com.kuts.klaf.data.common.notifications.NotificationChannelInitializer.Companion.DECK_REPETITION_CHANNEL_ID
import com.kuts.klaf.presentation.common.MainActivity
import com.kuts.klaf.presentation.deckRepetition.DECK_ID_NAVIGATION_ARGUMENT_KEY
import com.kuts.klaf.presentation.deckRepetition.DECK_NAME_NAVIGATION_ARGUMENT_KEY
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class DeckRepetitionNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationManager: NotificationManager
) {

    companion object {

        private const val DECK_REPETITION_GROUP_KEY = "deck_repetition_group"
        private const val COMMON_NOTIFICATION_ID = 435243543
        private const val MIN_NOTIFICATION_FOR_GROUP = 4
    }

    fun showNotification(deckName: String, deckId: Int) {
        val notification = createDeckRepetitionNotification(deckName = deckName, deckId = deckId)

        notificationManager.notify(deckId, notification)
        showSummeryNotificationIfSdkLessThan24AndMoreThan22()
    }

    fun showCommonNotification() {
        notificationManager.notify(COMMON_NOTIFICATION_ID, createCommonDeckRepetitionNotification())
    }

    fun removeNotificationFromNotificationBar(deckId: Int) {
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
            .setContentIntent(createPaddingIntent(deckId = deckId, deckName = deckName))
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
            .setContentIntent(createCommonPendingIntent())
            .build()
    }

    private fun createCommonDeckRepetitionNotification(): Notification {
        return  NotificationCompat.Builder(context, DECK_REPETITION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_deck_repetition_notification_24)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(
                context.getString(R.string.deck_repetition_common_notification_template)
            )
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(createCommonPendingIntent())
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

    private fun createPaddingIntent(deckId: Int, deckName: String): PendingIntent {
        return NavDeepLinkBuilder(context)
            .setComponentName(MainActivity::class.java)
            .setGraph(R.navigation.nav_graph)
            .setDestination(R.id.deckRepetitionFragment)
            .setArguments(
                bundleOf(
                    DECK_ID_NAVIGATION_ARGUMENT_KEY to deckId,
                    DECK_NAME_NAVIGATION_ARGUMENT_KEY to deckName,
                )
            )
            .createPendingIntent()
    }

    private fun createCommonPendingIntent(): PendingIntent {
        return NavDeepLinkBuilder(context)
            .setComponentName(MainActivity::class.java)
            .setGraph(R.navigation.nav_graph)
            .setDestination(R.id.deckListFragment)
            .createPendingIntent()
    }
}