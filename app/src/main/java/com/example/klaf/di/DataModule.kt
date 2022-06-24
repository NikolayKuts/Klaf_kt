package com.example.klaf.di

import android.content.Context
import com.example.klaf.data.room.databases.KlafRoomDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
class DataModule {

    @Provides
    fun provideKlafRoomDatabase(@ApplicationContext context: Context): KlafRoomDatabase {
        return KlafRoomDatabase.getInstance(context = context)
    }
}