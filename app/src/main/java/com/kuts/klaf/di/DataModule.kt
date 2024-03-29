package com.kuts.klaf.di

import android.content.Context
import android.net.ConnectivityManager
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import androidx.work.WorkManager
import com.kuts.klaf.data.dataStore.DECK_REPETITION_INFO_FILE_NAME
import com.kuts.domain.entities.DeckRepetitionInfos
import com.kuts.domain.repositories.CrashlyticsRepository
import com.kuts.klaf.data.dataStore.DeckRepetitionInfosSerializer
import com.kuts.klaf.data.networking.CardAudioPlayer
import com.kuts.klaf.data.room.databases.KlafRoomDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
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

    @Singleton
    @Provides
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Singleton
    @Provides
    fun provideCrashlytics(): FirebaseCrashlytics = Firebase.crashlytics

    @Provides
    fun provideCardAudioPlayer(
        crashlyticsRepository: CrashlyticsRepository
    ): CardAudioPlayer = CardAudioPlayer(crashlytics = crashlyticsRepository)

    @Provides
    fun provideConnectivityManager(@ApplicationContext context: Context): ConnectivityManager {
        return context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
}