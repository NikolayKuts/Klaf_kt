package com.example.klaf.di

import android.content.Context
import androidx.work.WorkManager
import com.example.klaf.data.room.databases.KlafRoomDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DataModule {

    @Provides
    @Singleton
    fun provideKlafRoomDatabase(@ApplicationContext context: Context): KlafRoomDatabase {
        return KlafRoomDatabase.getInstance(context = context)
    }

    @Provides
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }
}