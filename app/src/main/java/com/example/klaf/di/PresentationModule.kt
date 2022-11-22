package com.example.klaf.di

import android.app.NotificationManager
import android.content.Context
import com.example.klaf.presentation.interimDeck.BaseInterimDeckViewModel
import com.example.klaf.presentation.interimDeck.InterimDeckViewModel
import dagger.Binds
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
}