package com.example.klaf.di

import android.app.Notification
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import androidx.work.WorkManager
import com.example.data.dataStore.DECK_REPETITION_INFO_FILE_NAME
import com.example.data.dataStore.DeckRepetitionInfosSerializer
import com.example.data.room.databases.KlafRoomDatabase
import com.example.domain.entities.DeckRepetitionInfos
import com.example.klaf.presentation.common.notifications.AppRestartNotifier
import com.example.klaf.presentation.common.notifications.DataSynchronizationNotifier
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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

    @Provides
    fun provideFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }


    @Singleton
    @Provides
    fun provideDeckRepetitionInfoDataStore(
        @ApplicationContext appContext: Context,
    ): DataStore<DeckRepetitionInfos> {
        return DataStoreFactory.create(
            serializer = DeckRepetitionInfosSerializer,
            corruptionHandler = null,
            migrations = listOf(),
            scope = CoroutineScope(context = Dispatchers.IO + SupervisorJob()),
            produceFile = { appContext.dataStoreFile(fileName = DECK_REPETITION_INFO_FILE_NAME) }
        )
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