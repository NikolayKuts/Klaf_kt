package com.example.klaf.presentation.common

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.os.bundleOf
import androidx.navigation.NavDeepLinkBuilder
import com.example.klaf.R
import com.example.klaf.domain.common.DECK_ID_KEY
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class Notifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    companion object {

        private const val CHANNEL_ID = "channel_id"
        private const val DECK_REPETITION_GROUP_KEY = "deck_repetition_group"
        private const val SUMMERY_NOTIFICATION_ID = 435243543
        private const val MIN_NOTIFICATION_FOR_GROUP = 4
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createDeckRepetitionNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = context.getString(R.string.deck_repetition_channel_name)
            val descriptionText =
                context.getString(R.string.deck_repetition_channel_description)
            val importance = NotificationManager.IMPORTANCE_MAX

            val notificationChannel = NotificationChannel(
                CHANNEL_ID,
                channelName,
                importance
            ).apply {
                description = descriptionText
                lightColor = Color.GREEN
                enableLights(true)
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    fun showNotification(deckName: String, deckId: Int) {
        val notification = createDeckRepetitionNotification(deckName = deckName, deckId = deckId)

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        showSummeryNotificationIfSdkLessThan24()
    }

    private fun showSummeryNotificationIfSdkLessThan24() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            if (notificationManager.activeNotifications.size >= MIN_NOTIFICATION_FOR_GROUP) {
                val summeryNotification =
                    createSummeryNotification(notificationManager.activeNotifications.size)
                notificationManager.notify(SUMMERY_NOTIFICATION_ID, summeryNotification)
            }
        }
    }

    private fun createDeckRepetitionNotification(deckName: String, deckId: Int): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_deck_repetiton_notification_24)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(
                context.getString(R.string.deck_repetition_notification_template, deckName
                )
            )
            .setGroupIfSdkLessThan24(groupKey = DECK_REPETITION_GROUP_KEY)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(createPaddingIntent(deckId = deckId))
            .build()
    }

    private fun createSummeryNotification(notificationQuantity: Int): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_deck_repetiton_notification_24)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setGroup(DECK_REPETITION_GROUP_KEY)
            .setGroupSummary(true)
            .setContentText(
                context.getString(R.string.deck_repetition_summery_notification_content_text)
            )
            .setSummeryStyle(notificationQuantity = notificationQuantity)
            .setContentIntent(createSummeryPendingIntent())
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

    private fun createPaddingIntent(deckId: Int): PendingIntent {
        return NavDeepLinkBuilder(context)
            .setComponentName(MainActivity::class.java)
            .setGraph(R.navigation.nav_graph)
            .setDestination(R.id.repeatFragment)
            .setArguments(bundleOf(DECK_ID_KEY to deckId))
            .createPendingIntent()
    }

    private fun createSummeryPendingIntent(): PendingIntent {
        return NavDeepLinkBuilder(context)
            .setComponentName(MainActivity::class.java)
            .setGraph(R.navigation.nav_graph)
            .setDestination(R.id.deckListFragment)
            .createPendingIntent()
    }
}