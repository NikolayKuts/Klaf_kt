package com.kuts.klaf.di

import android.app.NotificationManager
import android.content.Context
import android.net.ConnectivityManager
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import androidx.work.WorkManager
import com.cambridge.dictionary.client.CambridgeClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.entities.DeckRepetitionInfos
import com.kuts.domain.managers.IAppMaintenanceManager
import com.kuts.domain.managers.IAudioPlayerManager
import com.kuts.domain.managers.IAuthenticationSessionManager
import com.kuts.domain.managers.IDeckReviewScheduler
import com.kuts.domain.managers.IWordInsightsProviderManager
import com.kuts.domain.repositories.IAuthenticationRepository
import com.kuts.domain.repositories.ICardRepository
import com.kuts.domain.repositories.ICrashlyticsRepository
import com.kuts.domain.repositories.IDeckRepetitionInfoRepository
import com.kuts.domain.repositories.IDeckRepository
import com.kuts.domain.repositories.IOldAppKlafDataTransferRepository
import com.kuts.domain.repositories.IStorageSaveVersionRepository
import com.kuts.domain.repositories.IWordAutocompleteRepository
import com.kuts.domain.repositories.IWordInfoRepository
import com.kuts.domain.repositories.IWordMeaningInsightsRepository
import com.kuts.klaf.cardManagement.common.ICambridgeWordDataProvider
import com.kuts.klaf.common.AndroidAppMaintenanceManager
import com.kuts.klaf.common.AndroidDeckReviewingReminder
import com.kuts.klaf.common.AndroidOldAppKlafDataTransferRepository
import com.kuts.klaf.common.AppReopeningWorker
import com.kuts.klaf.common.CoroutineContextProvider
import com.kuts.klaf.common.DataSynchronizationWorker
import com.kuts.klaf.common.DeckRepetitionReminder
import com.kuts.klaf.common.DeckRepetitionReminderChecker
import com.kuts.klaf.common.DeckReviewRescheduler
import com.kuts.klaf.common.NetworkConnectivity
import com.kuts.klaf.common.notifications.AppRestartNotifier
import com.kuts.klaf.common.notifications.DataSynchronizationNotifier
import com.kuts.klaf.common.notifications.NotificationChannelInitializer
import com.kuts.klaf.dataStore.DECK_REPETITION_INFO_FILE_NAME
import com.kuts.klaf.dataStore.DeckRepetitionInfosSerializer
import com.kuts.klaf.dataStore.implementations.DataStoreDeckRepetitionInfoRepository
import com.kuts.klaf.firestore.AndroidFirebaseAuthenticationSessionManager
import com.kuts.klaf.firestore.repositoryImplementations.AndroidAuthenticationRepositoryFirebase
import com.kuts.klaf.firestore.repositoryImplementations.AndroidCardRepositoryFirestore
import com.kuts.klaf.firestore.repositoryImplementations.AndroidCrashlyticsRepositoryFirebase
import com.kuts.klaf.firestore.repositoryImplementations.AndroidDeckRepositoryFirestore
import com.kuts.klaf.firestore.repositoryImplementations.AndroidStorageSaveVersionRepositoryFirestore
import com.kuts.klaf.firestore.repositoryImplementations.AndroidWordAutocompleteFirestore
import com.kuts.klaf.networking.AndroidCardAudioPlayer
import com.kuts.klaf.networking.codexApp.AndroidCodexAppHttpClientFactory
import com.kuts.klaf.networking.codexApp.CodexAppWordMeaningInsightsRepository
import com.kuts.klaf.networking.codexApp.ICodexAppHttpClientFactory
import com.kuts.klaf.networking.openai.OpenAiHttpClientFactory
import com.kuts.klaf.networking.openai.OpenAiWordMeaningInsightsRepository
import com.kuts.klaf.networking.wordInsights.SwitchableWordMeaningInsightsRepository
import com.kuts.klaf.networking.wordInsights.WordInsightsProviderManager
import com.kuts.klaf.networking.yandexApi.YandexSecureHttpClientFactory
import com.kuts.klaf.networking.yandexApi.YandexWordInfoRepository
import com.kuts.klaf.room.databases.KlafRoomDatabase
import com.kuts.klaf.room.databases.KlafRoomDatabaseProvider
import com.lib.lokdroid.core.LoKdroid
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.worker
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

internal val androidDataModule = module {
    androidRepositoryModule()
    infrastructureModule()
    dataManagerBindings()
    workerModule()
}

private fun Module.androidRepositoryModule() {
    single<IDeckRepository>(
        qualifier = named(name = REMOTE_DECK_REPOSITORY),
    ) {
        AndroidDeckRepositoryFirestore(
            firestore = get(),
            auth = get(),
        )
    }
    single<ICardRepository>(
        qualifier = named(name = REMOTE_CARD_REPOSITORY),
    ) {
        AndroidCardRepositoryFirestore(
            firestore = get(),
            auth = get(),
        )
    }
    single<IStorageSaveVersionRepository>(
        qualifier = named(name = REMOTE_STORAGE_SAVE_VERSION_REPOSITORY),
    ) {
        AndroidStorageSaveVersionRepositoryFirestore(
            firestore = get(),
            auth = get(),
        )
    }

    single<IDeckRepetitionInfoRepository> {
        DataStoreDeckRepetitionInfoRepository(
            dataStore = get(qualifier = named(name = DECK_REPETITION_INFOS_DATA_STORE)),
        )
    }
    single<IWordAutocompleteRepository> { AndroidWordAutocompleteFirestore(firestore = get()) }
    single<ICrashlyticsRepository> { AndroidCrashlyticsRepositoryFirebase(firebaseCrashlytics = get()) }
    single<IAuthenticationRepository> {
        AndroidAuthenticationRepositoryFirebase(
            auth = get(),
            crashlytics = get(),
        )
    }
    single<IAuthenticationSessionManager> { AndroidFirebaseAuthenticationSessionManager(auth = get()) }
    single<IWordInfoRepository> {
        YandexWordInfoRepository(
            client = YandexSecureHttpClientFactory().create(),
        )
    }
    single {
        OpenAiWordMeaningInsightsRepository(
            client = OpenAiHttpClientFactory().create(),
        )
    }
    single {
        WordInsightsProviderManager(
            dataStore = get(qualifier = named(name = APP_PREFERENCES_DATA_STORE)),
            codexClient = get(qualifier = named(name = CODEX_APP_HTTP_CLIENT)),
            coroutineContextProvider = get(),
            codexServerUrl = com.kuts.klaf.SecretConstants.CodexApp.appServerUrlOrNull().orEmpty(),
            codexModel = com.kuts.klaf.SecretConstants.CodexApp.modelOrNull(),
        )
    }
    single<IWordInsightsProviderManager> { get<WordInsightsProviderManager>() }
    single {
        CodexAppWordMeaningInsightsRepository(
            manager = get(),
        )
    }
    single<IWordMeaningInsightsRepository> {
        SwitchableWordMeaningInsightsRepository(
            manager = get(),
            openAiRepository = get(),
            codexRepository = get(),
        )
    }
    single<IOldAppKlafDataTransferRepository> {
        AndroidOldAppKlafDataTransferRepository(
            context = androidContext(),
            deckRepository = get(qualifier = named(name = LOCAL_DECK_REPOSITORY)),
            cardRepository = get(qualifier = named(name = LOCAL_CARD_REPOSITORY)),
        )
    }
}

private fun Module.infrastructureModule() {
    single<KlafRoomDatabase> { KlafRoomDatabaseProvider.getInstance(context = androidContext()) }
    single { WorkManager.getInstance(androidContext()) }
    single<ICoroutineContextProvider> { CoroutineContextProvider() }
    single<ICodexAppHttpClientFactory> { AndroidCodexAppHttpClientFactory() }
    single<HttpClient>(qualifier = named(name = CODEX_APP_HTTP_CLIENT)) {
        get<ICodexAppHttpClientFactory>().create()
    }

    single<IDeckReviewScheduler> { AndroidDeckReviewingReminder(context = androidContext()) }

    single { FirebaseFirestore.getInstance() }
    single { FirebaseAuth.getInstance() }
    single<FirebaseCrashlytics> { Firebase.crashlytics }

    single<DataStore<DeckRepetitionInfos>>(
        qualifier = named(name = DECK_REPETITION_INFOS_DATA_STORE),
    ) {
        DataStoreFactory.create(
            serializer = DeckRepetitionInfosSerializer,
            corruptionHandler = null,
            migrations = listOf(),
            scope = CoroutineScope(context = Dispatchers.IO + SupervisorJob()),
            produceFile = { androidContext().dataStoreFile(fileName = DECK_REPETITION_INFO_FILE_NAME) },
        )
    }

    single {
        androidContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
    single {
        androidContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    single { CambridgeClient }
    single<ICambridgeWordDataProvider> { AndroidCambridgeWordDataProvider(client = get()) }
    single { LoKdroid }
}

private fun Module.dataManagerBindings() {
    single {
        NotificationChannelInitializer(
            context = androidContext(),
            notificationManager = get(),
        )
    }
    single { AppRestartNotifier(context = androidContext()) }
    single { DataSynchronizationNotifier(context = androidContext()) }
    single { NetworkConnectivity(connectivityManager = get()) }

    single<IAppMaintenanceManager> {
        AndroidAppMaintenanceManager(
            workManager = get(),
            notificationChannelInitializer = get(),
            networkConnectivity = get(),
        )
    }

    factory<IAudioPlayerManager> { AndroidCardAudioPlayer(crashlytics = get()) }
}

private fun Module.workerModule() {
    worker {
        AppReopeningWorker(
            application = get(),
            workerParams = get(),
            appRestartNotifier = get(),
        )
    }
    worker {
        DeckRepetitionReminder(
            appContext = get(),
            parameters = get(),
            deckReviewNotifier = get(),
        )
    }
    worker {
        DeckRepetitionReminderChecker(
            context = get(),
            params = get(),
            deckReviewNotifier = get(),
            fetchAllDecks = get(),
            crashlytics = get(),
        )
    }
    worker {
        DataSynchronizationWorker(
            appContext = get(),
            params = get(),
            synchronizeLocalAndRemoteData = get(),
            dataSynchronizationNotifier = get(),
            crashlytics = get(),
        )
    }
    worker {
        DeckReviewRescheduler(
            appContext = get(),
            parameters = get(),
            fetchAllDecksUseCase = get(),
            deckReviewingReminder = get<IDeckReviewScheduler>(),
        )
    }
}
