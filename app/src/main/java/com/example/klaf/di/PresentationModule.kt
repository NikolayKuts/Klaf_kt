package com.example.klaf.di

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import com.example.presentation.common.notifications.AppRestartNotifier
import com.example.presentation.common.notifications.DataSynchronizationNotifier
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
class PresentationModule {

    @Provides
    fun provideNotificationManager(@ApplicationContext context: Context): NotificationManager {
        return context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    @Provides
    @com.example.data.common.AppRestartNotification
    fun provideAppReopeningNotification(notifier: AppRestartNotifier): Notification {
        return notifier.createAppRestartNotification()
    }

    @Provides
    @com.example.data.common.DataSynchronizationNotification
    fun provideDataSynchronizationNotification(notifier: DataSynchronizationNotifier): Notification {
        return notifier.createNotification()
    }
}