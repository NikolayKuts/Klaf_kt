package com.kuts.klaf.di

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.net.ConnectivityManager
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import androidx.work.WorkManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.klaf.SecretConstants
import com.kuts.domain.entities.DeckRepetitionInfos
import com.kuts.domain.managers.IKlafServerConnectionManager
import com.kuts.domain.managers.IAppMaintenanceManager
import com.kuts.domain.managers.IAudioPlayerManager
import com.kuts.domain.managers.IAuthenticationSessionManager
import com.kuts.domain.managers.IAccountSession
import com.kuts.domain.managers.IDeckReviewScheduler
import com.kuts.domain.managers.IReviewReminderScopeProvider
import com.kuts.domain.managers.IAccountScopedDeckReviewNotifier
import com.kuts.domain.managers.IMnemonicGenerationBackgroundManager
import com.kuts.domain.managers.ISpeechRecognitionManager
import com.kuts.domain.managers.ITextToSpeechManager
import com.kuts.domain.managers.IVocabularySourceAnalysisBackgroundManager
import com.kuts.domain.repositories.IAuthenticationRepository
import com.kuts.domain.repositories.ICardRepository
import com.kuts.domain.repositories.ICrashlyticsRepository
import com.kuts.domain.repositories.IDeckRepetitionInfoRepository
import com.kuts.domain.repositories.IDeckRepository
import com.kuts.domain.repositories.IMnemonicAssociationRepository
import com.kuts.domain.repositories.IMnemonicImageAssetRepository
import com.kuts.domain.repositories.IMnemonicImageRepository
import com.kuts.domain.repositories.IMnemonicImageRemoteRepository
import com.kuts.domain.repositories.IOldAppKlafDataTransferRepository
import com.kuts.domain.repositories.IStorageSaveVersionRepository
import com.kuts.domain.repositories.IWordAutocompleteRepository
import com.kuts.domain.repositories.IWordInfoRepository
import com.kuts.domain.repositories.IWordMeaningInsightsRepository
import com.kuts.klaf.cardManagement.common.ICambridgeWordDataProvider
import com.kuts.klaf.common.AndroidAppMaintenanceManager
import com.kuts.klaf.common.AndroidDeckReviewingReminder
import com.kuts.klaf.common.AndroidScopedDeckReminderActions
import com.kuts.klaf.common.AndroidScopedReminderStartupRecovery
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
import com.kuts.klaf.firebaseStorage.AndroidMnemonicImageRemoteRepository
import com.kuts.klaf.firestore.AndroidFirebaseAuthenticationSessionManager
import com.kuts.klaf.firestore.repositoryImplementations.AndroidAuthenticationRepositoryFirebase
import com.kuts.klaf.firestore.repositoryImplementations.AndroidCardRepositoryFirestore
import com.kuts.klaf.firestore.repositoryImplementations.AndroidCrashlyticsRepositoryFirebase
import com.kuts.klaf.firestore.repositoryImplementations.AndroidDeckRepositoryFirestore
import com.kuts.klaf.firestore.repositoryImplementations.AndroidStorageSaveVersionRepositoryFirestore
import com.kuts.klaf.firestore.repositoryImplementations.AndroidWordAutocompleteFirestore
import com.kuts.klaf.networking.AndroidCardAudioPlayer
import com.kuts.klaf.mnemonic.AndroidMnemonicImageAssetRepository
import com.kuts.klaf.mnemonic.CachedMnemonicImageAssetRepository
import com.kuts.klaf.networking.klafServer.KlafServerImageRestClient
import java.io.File
import java.security.MessageDigest
import com.kuts.klaf.mnemonic.AndroidApplicationVisibilityTracker
import com.kuts.klaf.mnemonic.AndroidMnemonicGenerationBackgroundManager
import com.kuts.klaf.mnemonic.AndroidMnemonicGenerationDiagnostics
import com.kuts.klaf.mnemonic.MnemonicGenerationNotifier
import com.kuts.klaf.networking.klafServer.AndroidKlafServerForegroundReconnecter
import com.kuts.klaf.networking.klafServer.AndroidAccountDeviceStore
import com.kuts.klaf.networking.klafServer.AccountDeviceIdentity
import com.kuts.klaf.networking.klafServer.AndroidPendingSignUpAttemptStore
import com.kuts.klaf.networking.klafServer.IKlafServerSession
import com.kuts.klaf.networking.klafServer.KlafServerConnectionManager
import com.kuts.klaf.networking.klafServer.KlafServerHttpClientFactory
import com.kuts.klaf.networking.klafServer.KlafServerAccountRestClient
import com.kuts.klaf.networking.klafServer.KlafServerSyncRestClient
import com.kuts.klaf.networking.klafServer.KlafServerSyncEventConnector
import com.kuts.klaf.networking.klafServer.SyncEventConnector
import com.kuts.klaf.networking.klafServer.KlafServerMnemonicAssociationRepository
import com.kuts.klaf.networking.klafServer.KlafServerMnemonicImageRepository
import com.kuts.klaf.networking.klafServer.KlafServerSession
import com.kuts.klaf.networking.klafServer.PersistentAccountDeviceProvider
import com.kuts.klaf.networking.klafServer.PendingSignUpAttemptStore
import com.kuts.klaf.networking.klafServer.ServerAccountSession
import com.kuts.klaf.networking.klafServer.createAndroidSecureAccountSession
import com.kuts.klaf.networking.klafServer.createAndroidAuthenticatedRequestSigner
import com.kuts.klaf.networking.klafServer.KlafAuthenticatedRequestSigner
import com.kuts.klaf.networking.klafServer.KlafServerWordMeaningInsightsRepository
import com.kuts.klaf.networking.yandexApi.YandexSecureHttpClientFactory
import com.kuts.klaf.networking.yandexApi.YandexWordInfoRepository
import com.kuts.klaf.push.AndroidKlafServerPushTokenRegistrar
import com.kuts.klaf.push.KlafPushTokenManager
import com.kuts.klaf.room.databases.ActiveLocalRoomDatabase
import com.kuts.klaf.room.databases.AndroidSelectedAccountStore

import com.kuts.klaf.room.databases.KlafRoomDatabase
import com.kuts.klaf.room.databases.KlafRoomDatabaseProvider
import com.kuts.klaf.room.databases.RoomDatabaseSource
import com.kuts.klaf.room.databases.RoomReminderSelectionObserver
import com.kuts.klaf.room.databases.ScopedDeckReminderActions
import com.kuts.klaf.room.databases.ScopedKlafRoomDatabaseFactory
import com.kuts.klaf.room.databases.StaticRoomDatabaseSource
import com.kuts.klaf.room.repositoryImplementations.DeckReviewInfoRepositoryRoom
import com.kuts.klaf.room.repositoryImplementations.GuestAccountDataTransfer
import com.kuts.klaf.speech.AndroidSpeechRecognitionManager
import com.kuts.klaf.speech.AndroidTextToSpeechManager
import com.kuts.domain.managers.IVocabularySourceTranscriptionBackgroundManager
import com.kuts.klaf.vocabularySource.AndroidVocabularySourceAnalysisBackgroundManager
import com.kuts.klaf.vocabularySource.AndroidVocabularySourceTranscriptionBackgroundManager
import com.kuts.klaf.vocabularySource.VocabularySourceAnalysisNotifier
import com.kuts.klaf.vocabularySource.VocabularySourceTranscriptionNotifier
import com.lib.lokdroid.core.LoKdroid
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
        val databaseSource = get<RoomDatabaseSource>()
        if (databaseSource is ActiveLocalRoomDatabase) {
            DeckReviewInfoRepositoryRoom(databaseSource)
        } else {
            DataStoreDeckRepetitionInfoRepository(
                dataStore = get(qualifier = named(name = DECK_REPETITION_INFOS_DATA_STORE)),
            )
        }
    }
    single<IWordAutocompleteRepository> {
        selectWordAutocompleteRepository(get<RoomDatabaseSource>() is ActiveLocalRoomDatabase) {
            AndroidWordAutocompleteFirestore(firestore = get())
        }
    }
    single<ICrashlyticsRepository> { AndroidCrashlyticsRepositoryFirebase(firebaseCrashlytics = get()) }
    single<IAuthenticationRepository> {
        AndroidAuthenticationRepositoryFirebase(
            auth = get(),
            crashlytics = get(),
        )
    }
    single<IAuthenticationSessionManager> { AndroidFirebaseAuthenticationSessionManager(auth = get()) }
    single { PersistentAccountDeviceProvider(AndroidAccountDeviceStore(androidContext())) }
    single {
        AccountDeviceIdentity(
            provider = get(),
            name = android.os.Build.MODEL.orEmpty().ifBlank { "Android" },
            platform = "ANDROID",
        )
    }
    single<PendingSignUpAttemptStore> { AndroidPendingSignUpAttemptStore(androidContext()) }
    single { KlafServerAccountRestClient(get<KlafServerEndpointConfig>().restBaseUrl(),
        KlafServerHttpClientFactory().create()) }
    single<KlafAuthenticatedRequestSigner> {
        createAndroidAuthenticatedRequestSigner(get<KlafServerEndpointConfig>().restBaseUrl(), androidContext(),
            KlafServerHttpClientFactory().create())
    }
    single { KlafServerSyncRestClient(get<KlafServerEndpointConfig>().restBaseUrl(),
        KlafServerHttpClientFactory().create(), get()) }
    single<IAccountSession> {
        val identity = get<AccountDeviceIdentity>()
        createAndroidSecureAccountSession(
            serverOrigin = get<KlafServerEndpointConfig>().restBaseUrl(),
            httpClient = KlafServerHttpClientFactory().create(),
            context = androidContext(),
            localDatabase = get(),
            guestTransfer = GuestAccountDataTransfer(get()),
            deviceProvider = identity::current,
            pendingEnrollment = get(),
        )
    }
    single<IWordInfoRepository> {
        YandexWordInfoRepository(
            client = YandexSecureHttpClientFactory().create(),
        )
    }
    single<IKlafServerSession> {
        val endpoint = get<KlafServerEndpointConfig>()
        KlafServerSession(
            host = endpoint.host,
            port = endpoint.port,
            isSecure = endpoint.isSecure,
            httpClient = KlafServerHttpClientFactory().create(),
            authenticatedRequestSigner = get<KlafAuthenticatedRequestSigner>(),
            selectedAccountEmail = get<IAccountSession>().selectedAccountEmail,
        )
    }
    single<IKlafServerConnectionManager> {
        KlafServerConnectionManager(
            klafServerSession = get(),
            coroutineContextProvider = get(),
            selectedAccountEmail = get<IAccountSession>().selectedAccountEmail,
            sameAccountSignInEpoch = get<IAccountSession>().sameAccountSignInEpoch,
        )
    }
    single(createdAtStart = true) {
        AndroidKlafServerForegroundReconnecter(
            applicationVisibilityTracker = get(),
            connectionManager = get(),
            coroutineContextProvider = get(),
            diagnostics = get(),
        )
    }
    single(createdAtStart = true) {
        AndroidKlafServerPushTokenRegistrar(
            klafServerSession = get(),
            pushTokenManager = get(),
            coroutineContextProvider = get(),
        )
    }
    single<IWordMeaningInsightsRepository> {
        KlafServerWordMeaningInsightsRepository(
            klafServerSession = get(),
        )
    }
    single<IMnemonicAssociationRepository> {
        KlafServerMnemonicAssociationRepository(klafServerSession = get())
    }
    single<IMnemonicImageRepository> {
        KlafServerMnemonicImageRepository(klafServerSession = get())
    }
    single<CachedMnemonicImageAssetRepository> {
        val context = androidContext()
        val local = AndroidMnemonicImageAssetRepository(context)
        val source = get<ActiveLocalRoomDatabase>()
        val identity = get<AccountDeviceIdentity>()
        val remote = get<KlafServerImageRestClient>()
        CachedMnemonicImageAssetRepository(
            local = local,
            selectedEmail = { source.selection.value.accountEmail },
            cacheForAccount = { email ->
                val key = MessageDigest.getInstance("SHA-256").digest(email.toByteArray())
                    .joinToString("") { "%02x".format(it) }
                AndroidMnemonicImageAssetRepository(context, File(context.filesDir, "mnemonic-remote-cache/$key"))
            },
            download = { email, asset -> remote.download(email, identity.current().id, asset) },
            ioContext = get<ICoroutineContextProvider>().io,
        )
    }
    single<IMnemonicImageAssetRepository> {
        if (get<RoomDatabaseSource>() is ActiveLocalRoomDatabase) get<CachedMnemonicImageAssetRepository>()
        else AndroidMnemonicImageAssetRepository(androidContext())
    }
    single<IMnemonicImageRemoteRepository> {
        AndroidMnemonicImageRemoteRepository(
            storage = get(),
            auth = get(),
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
    single {
        KlafServerEndpointConfig(
            host = SecretConstants.KlafServer.HOST,
            port = SecretConstants.KlafServer.PORT,
            isSecure = SecretConstants.KlafServer.IS_SECURE,
        )
    }
    single<SyncEventConnector> {
        KlafServerSyncEventConnector(
            get<KlafServerEndpointConfig>().restBaseUrl(), KlafServerHttpClientFactory().create(), get(),
        )
    }
    single { KlafServerImageRestClient(get<KlafServerEndpointConfig>().restBaseUrl(),
        KlafServerHttpClientFactory().create(), get<KlafAuthenticatedRequestSigner>()) }
    single<KlafRoomDatabase> { KlafRoomDatabaseProvider.getInstance(context = androidContext()) }
    single<RoomDatabaseSource> {
        val context = androidContext()
        val useScopedStorage = useAccountScopedStorage(
            requestedMode = BuildConfig.KLAF_CLIENT_STORAGE_MODE,
            isolatedTestIdentity = context.packageName == "com.kuts.klaf.remote.storage.test",
            legacyDatabaseExists = context.getDatabasePath("klaf_kt.db").exists(),
        )
        if (useScopedStorage) get<ActiveLocalRoomDatabase>() else StaticRoomDatabaseSource(database = get())
    }
    single<ActiveLocalRoomDatabase> {
        ActiveLocalRoomDatabase(
            factory = ScopedKlafRoomDatabaseFactory(context = androidContext()),
            accountStore = AndroidSelectedAccountStore(context = androidContext()),
            selectionObserver = RoomReminderSelectionObserver(reminders = get()),
            beforeSelectionChange = {
                get<com.kuts.domain.managers.VocabularySourceTranscriptionCoordinator>().cancelAll()
                get<IKlafServerSession>().endUserSession()
                (androidContext().getSystemService(android.content.Context.NOTIFICATION_SERVICE) as
                    android.app.NotificationManager).cancelAll()
            },
        )
    }
    single<ScopedDeckReminderActions> { AndroidScopedDeckReminderActions(context = androidContext()) }
    single<IReviewReminderScopeProvider> { AndroidSelectedAccountStore(context = androidContext()) }
    single { AndroidSelectedAccountStore(context = androidContext()) }
    single { WorkManager.getInstance(androidContext()) }
    single(createdAtStart = true) {
        AndroidScopedReminderStartupRecovery(selectedAccount = get(), workManager = get())
    }
    single<ICoroutineContextProvider> { CoroutineContextProvider() }
    single<IDeckReviewScheduler> { AndroidDeckReviewingReminder(context = androidContext()) }

    single { FirebaseFirestore.getInstance() }
    single { FirebaseAuth.getInstance() }
    single { FirebaseStorage.getInstance() }
    single { FirebaseMessaging.getInstance() }
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

    // The Cambridge client is out of action until it is rebuilt against Ktor 3 -- see
    // AndroidNoOpCambridgeWordDataProvider. Creating it crashed the app on any screen that
    // resolved it, so it is not constructed at all.
    single<ICambridgeWordDataProvider> { AndroidNoOpCambridgeWordDataProvider() }
    single { LoKdroid }
    single { AndroidMnemonicGenerationDiagnostics(context = androidContext()) }
    single(createdAtStart = true) {
        AndroidApplicationVisibilityTracker(
            application = androidContext().applicationContext as Application,
            diagnostics = get(),
        )
    }
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
    single {
        MnemonicGenerationNotifier(
            context = androidContext(),
            notificationManager = get(),
            sessionIdProvider = { get<IKlafServerSession>().clientSessionId },
        )
    }
    single {
        VocabularySourceAnalysisNotifier(
            context = androidContext(),
            notificationManager = get(),
            sessionIdProvider = { get<IKlafServerSession>().clientSessionId },
        )
    }
    single {
        VocabularySourceTranscriptionNotifier(
            context = androidContext(),
            notificationManager = get(),
            sessionIdProvider = { get<IKlafServerSession>().clientSessionId },
        )
    }
    single {
        KlafPushTokenManager(
            firebaseMessaging = get(),
            coroutineContextProvider = get(),
        )
    }
    single<IMnemonicGenerationBackgroundManager> {
        AndroidMnemonicGenerationBackgroundManager(
            context = androidContext(),
            applicationVisibilityTracker = get(),
            diagnostics = get(),
            notificationChannelInitializer = get(),
            notifier = get(),
        )
    }
    single<IVocabularySourceAnalysisBackgroundManager> {
        AndroidVocabularySourceAnalysisBackgroundManager(
            context = androidContext(),
            applicationVisibilityTracker = get(),
            diagnostics = get(),
            notificationChannelInitializer = get(),
            notifier = get(),
        )
    }
    single<IVocabularySourceTranscriptionBackgroundManager> {
        AndroidVocabularySourceTranscriptionBackgroundManager(
            context = androidContext(),
            applicationVisibilityTracker = get(),
            diagnostics = get(),
            notificationChannelInitializer = get(),
            notifier = get(),
        )
    }
    single { NetworkConnectivity(connectivityManager = get()) }

    single<IAppMaintenanceManager> {
        AndroidAppMaintenanceManager(
            workManager = get(),
            notificationChannelInitializer = get(),
            networkConnectivity = get(),
            mnemonicImageAssetRepository = get(),
        )
    }

    factory<IAudioPlayerManager> { AndroidCardAudioPlayer(crashlytics = get()) }
    factory<ITextToSpeechManager> { AndroidTextToSpeechManager(context = androidContext()) }
    factory<ISpeechRecognitionManager> {
        AndroidSpeechRecognitionManager(context = androidContext())
    }
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
            selectedAccount = get(),
            scopedNotifier = get(),
        )
    }
    worker {
        DeckRepetitionReminderChecker(
            context = get(),
            params = get(),
            scopedNotifier = get<IAccountScopedDeckReviewNotifier>(),
            fetchAllDecks = get(),
            crashlytics = get(),
            selectedAccount = get(),
            activeLocalDatabase = get(),
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
            selectedAccount = get(),
            activeLocalDatabase = get(),
        )
    }
}
