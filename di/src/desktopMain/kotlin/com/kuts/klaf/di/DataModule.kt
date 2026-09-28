package com.kuts.klaf.di

import com.kuts.domain.common.AuthenticationAction
import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.common.IDataSynchronizationState
import com.kuts.domain.common.LoadingState
import com.kuts.domain.entities.AuthenticationState
import com.kuts.domain.entities.AutocompleteWord
import com.kuts.domain.entities.DeckRepetitionInfo
import com.kuts.domain.entities.WordMeaningInsights
import com.kuts.domain.managers.IKlafServerConnectionManager
import com.kuts.domain.managers.IAppMaintenanceManager
import com.kuts.domain.managers.IAudioPlayerManager
import com.kuts.domain.managers.IAuthenticationSessionManager
import com.kuts.domain.managers.IAccountSession
import com.kuts.domain.managers.IDeckReviewScheduler
import com.kuts.domain.managers.IMnemonicGenerationBackgroundManager
import com.kuts.domain.managers.ISpeechRecognitionManager
import com.kuts.domain.managers.IVocabularySourceAnalysisBackgroundManager
import com.kuts.domain.managers.SpeechRecognitionResult
import com.kuts.domain.managers.SpeechRecognitionState
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
import com.kuts.klaf.common.CoroutineContextProvider
import com.kuts.klaf.mnemonic.DesktopNoOpMnemonicImageRemoteRepository
import com.kuts.klaf.mnemonic.DesktopMnemonicImageAssetRepository
import com.kuts.klaf.mnemonic.CachedMnemonicImageAssetRepository
import com.kuts.klaf.networking.klafServer.KlafServerImageRestClient
import java.security.MessageDigest
import com.kuts.klaf.mnemonic.NoOpMnemonicGenerationBackgroundManager
import com.kuts.klaf.networking.klafServer.IKlafServerSession
import com.kuts.klaf.networking.klafServer.DesktopAccountDeviceStore
import com.kuts.klaf.networking.klafServer.AccountDeviceIdentity
import com.kuts.klaf.networking.klafServer.DesktopPendingSignUpAttemptStore
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
import com.kuts.klaf.networking.klafServer.KlafServerWordMeaningInsightsRepository
import com.kuts.klaf.networking.yandexApi.YandexSecureHttpClientFactory
import com.kuts.klaf.networking.yandexApi.YandexWordInfoRepository
import com.kuts.klaf.room.databases.ActiveLocalRoomDatabase
import com.kuts.klaf.room.databases.DesktopSelectedAccountStore
import com.kuts.klaf.room.databases.KlafRoomDatabase
import com.kuts.klaf.room.databases.KlafRoomDatabaseProvider
import com.kuts.klaf.room.databases.RoomDatabaseSource
import com.kuts.klaf.room.databases.ScopedKlafRoomDatabaseFactory
import com.kuts.klaf.room.databases.StaticRoomDatabaseSource
import com.kuts.klaf.room.repositoryImplementations.DeckReviewInfoRepositoryRoom
import com.kuts.klaf.room.repositoryImplementations.GuestAccountDataTransfer
import com.kuts.klaf.vocabularySource.NoOpVocabularySourceAnalysisBackgroundManager
import java.io.File
import java.util.Properties
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

internal fun findDeveloperLocalProperties(): File? {
    var directory = File(System.getProperty("user.dir")).absoluteFile
    repeat(4) {
        if (File(directory, "settings.gradle.kts").isFile) {
            return File(directory, "local.properties").takeIf(File::isFile)
        }
        directory = directory.parentFile ?: return null
    }
    return null
}

internal val desktopDataModule = module {
    desktopRepositoryModule()
    desktopInfrastructureModule()
    desktopManagerBindings()
}

private fun Module.desktopRepositoryModule() {
    single<IDeckRepository>(
        qualifier = named(name = REMOTE_DECK_REPOSITORY),
    ) {
        get(qualifier = named(name = LOCAL_DECK_REPOSITORY))
    }
    single<ICardRepository>(
        qualifier = named(name = REMOTE_CARD_REPOSITORY),
    ) {
        get(qualifier = named(name = LOCAL_CARD_REPOSITORY))
    }
    single<IStorageSaveVersionRepository>(
        qualifier = named(name = REMOTE_STORAGE_SAVE_VERSION_REPOSITORY),
    ) {
        get(qualifier = named(name = LOCAL_STORAGE_SAVE_VERSION_REPOSITORY))
    }

    single<DesktopAuthenticationRepository> { DesktopAuthenticationRepository() }
    single<IAuthenticationRepository> { get<DesktopAuthenticationRepository>() }
    single<IAuthenticationSessionManager> {
        DesktopAuthenticationSessionManager(authenticationRepository = get())
    }
    single { PersistentAccountDeviceProvider(DesktopAccountDeviceStore(get<DesktopStorageConfiguration>().directory)) }
    single { AccountDeviceIdentity(provider = get(), name = "Desktop", platform = "DESKTOP") }
    single<PendingSignUpAttemptStore> {
        DesktopPendingSignUpAttemptStore(get<DesktopStorageConfiguration>().directory)
    }
    single { KlafServerAccountRestClient(get<KlafServerEndpointConfig>().restBaseUrl(),
        KlafServerHttpClientFactory().create()) }
    single { KlafServerSyncRestClient(get<KlafServerEndpointConfig>().restBaseUrl(),
        KlafServerHttpClientFactory().create()) }
    single<IAccountSession> {
        val identity = get<AccountDeviceIdentity>()
        ServerAccountSession(
            accounts = get(),
            localDatabase = get(),
            guestTransfer = GuestAccountDataTransfer(get()),
            deviceProvider = identity::current,
            pendingSignUp = get(),
        )
    }
    single<IWordInfoRepository> {
        YandexWordInfoRepository(
            client = YandexSecureHttpClientFactory().create(),
        )
    }
    single<IWordAutocompleteRepository> { DesktopWordAutocompleteRepository() }
    single<IKlafServerSession> {
        val endpoint = get<KlafServerEndpointConfig>()
        KlafServerSession(
            host = endpoint.host,
            port = endpoint.port,
            isSecure = endpoint.isSecure,
            httpClient = KlafServerHttpClientFactory().create(),
        )
    }
    single<IKlafServerConnectionManager> {
        KlafServerConnectionManager(
            klafServerSession = get(),
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
    single<IMnemonicImageAssetRepository> {
        val directory = get<DesktopStorageConfiguration>().directory
        val local = DesktopMnemonicImageAssetRepository(directory)
        if (!get<DesktopStorageConfiguration>().useAccountScopedStorage) local else {
            val source = get<ActiveLocalRoomDatabase>()
            val identity = get<AccountDeviceIdentity>()
            val remote = get<KlafServerImageRestClient>()
            CachedMnemonicImageAssetRepository(
                local = local,
                selectedEmail = { source.selection.value.accountEmail },
                cacheForAccount = { email ->
                    val key = MessageDigest.getInstance("SHA-256").digest(email.toByteArray())
                        .joinToString("") { "%02x".format(it) }
                    DesktopMnemonicImageAssetRepository(File(directory, "mnemonic-remote-cache/$key"))
                },
                download = { email, asset -> remote.download(email, identity.current().id, asset) },
                ioContext = get<ICoroutineContextProvider>().io,
            )
        }
    }
    single<IMnemonicImageRemoteRepository> { DesktopNoOpMnemonicImageRemoteRepository() }
    single { KlafServerImageRestClient(get<KlafServerEndpointConfig>().restBaseUrl(), KlafServerHttpClientFactory().create()) }
    single<IDeckRepetitionInfoRepository> {
        val databaseSource = get<RoomDatabaseSource>()
        if (databaseSource is ActiveLocalRoomDatabase) {
            DeckReviewInfoRepositoryRoom(databaseSource)
        } else {
            DesktopInMemoryDeckRepetitionInfoRepository()
        }
    }
    single<IOldAppKlafDataTransferRepository> { DesktopNoOpOldAppKlafDataTransferRepository() }
    single<ICrashlyticsRepository> { DesktopNoOpCrashlyticsRepository() }
}

private fun Module.desktopInfrastructureModule() {
    single { DesktopStorageConfiguration.fromEnvironment() }
    single {
        val properties = Properties().apply {
            findDeveloperLocalProperties()?.inputStream()?.use(::load)
        }
        KlafServerEndpointConfig(
            host = properties.getProperty("klaf.client.server.host", "127.0.0.1").trim(),
            port = properties.getProperty("klaf.client.server.port", "8090").trim().toInt(),
        )
    }
    single<KlafRoomDatabase> { KlafRoomDatabaseProvider.getInstance() }
    single<SyncEventConnector> {
        KlafServerSyncEventConnector(
            get<KlafServerEndpointConfig>().restBaseUrl(), KlafServerHttpClientFactory().create(),
        )
    }
    single<RoomDatabaseSource> {
        if (get<DesktopStorageConfiguration>().useAccountScopedStorage) get<ActiveLocalRoomDatabase>()
        else StaticRoomDatabaseSource(database = get())
    }
    single<ActiveLocalRoomDatabase> {
        ActiveLocalRoomDatabase(
            factory = ScopedKlafRoomDatabaseFactory(get<DesktopStorageConfiguration>().directory),
            accountStore = DesktopSelectedAccountStore(get<DesktopStorageConfiguration>().directory),
        )
    }
    single<ICoroutineContextProvider> { CoroutineContextProvider() }
}

private fun Module.desktopManagerBindings() {
    single<IAppMaintenanceManager> { DesktopAppMaintenanceManager(mnemonicImageAssetRepository = get()) }
    single<IMnemonicGenerationBackgroundManager> {
        NoOpMnemonicGenerationBackgroundManager()
    }
    single<IVocabularySourceAnalysisBackgroundManager> {
        NoOpVocabularySourceAnalysisBackgroundManager()
    }
    factory<IAudioPlayerManager> { DesktopNoOpAudioPlayerManager() }
    factory<ISpeechRecognitionManager> { DesktopNoOpSpeechRecognitionManager() }
    single<IDeckReviewScheduler> { DesktopNoOpDeckReviewScheduler() }
}

private class DesktopAuthenticationRepository : IAuthenticationRepository {
    private val state = MutableStateFlow(AuthenticationState(email = null))

    val currentState: AuthenticationState
        get() = state.value

    override val authenticationState: Flow<AuthenticationState> = state

    override fun signInWithEmailAndPassword(
        email: String,
        password: String,
    ): Flow<LoadingState<AuthenticationAction, IAuthenticationRepository.IAuthenticationError>> {
        state.value = AuthenticationState(email = email)
        return flowOf(LoadingState.Success(data = AuthenticationAction.SIGN_IN))
    }

    override fun signUpWithEmailAndPassword(
        email: String,
        password: String,
    ): Flow<LoadingState<AuthenticationAction, IAuthenticationRepository.IAuthenticationError>> {
        state.value = AuthenticationState(email = email)
        return flowOf(LoadingState.Success(data = AuthenticationAction.SIGN_UP))
    }

    override fun signOut(): Flow<LoadingState<Unit, IAuthenticationRepository.IAuthenticationError>> {
        state.value = AuthenticationState(email = null)
        return flowOf(LoadingState.Success(data = Unit))
    }

    override fun deleteProfile(): Flow<LoadingState<Unit, IAuthenticationRepository.IAuthenticationError>> {
        state.value = AuthenticationState(email = null)
        return flowOf(LoadingState.Success(data = Unit))
    }

    override fun reauthenticateWithEmailAndPassword(
        email: String,
        password: String,
    ): Flow<LoadingState<AuthenticationAction, IAuthenticationRepository.IAuthenticationError>> {
        return flowOf(LoadingState.Success(data = AuthenticationAction.SIGN_IN))
    }
}

private class DesktopAuthenticationSessionManager(
    private val authenticationRepository: DesktopAuthenticationRepository,
) : IAuthenticationSessionManager {

    override fun isSignedIn(): Boolean = authenticationRepository.currentState.email != null
}

private class DesktopWordAutocompleteRepository : IWordAutocompleteRepository {
    override val isEnabled: Boolean = false

    override suspend fun fetchAutocomplete(prefix: String): List<AutocompleteWord> = emptyList()
}

private class DesktopWordMeaningInsightsRepository : IWordMeaningInsightsRepository {
    override suspend fun fetchWordMeaningInsights(word: String): WordMeaningInsights {
        return WordMeaningInsights.EMPTY
    }
}

private class DesktopInMemoryDeckRepetitionInfoRepository : IDeckRepetitionInfoRepository {
    private val source = MutableStateFlow<Map<Int, DeckRepetitionInfo>>(emptyMap())

    override fun fetchDeckRepetitionInfo(deckId: Int): Flow<DeckRepetitionInfo?> {
        return source.map { infos -> infos[deckId] }
    }

    override suspend fun saveDeckRepetitionInfo(info: DeckRepetitionInfo) {
        source.update { infos -> infos + (info.deckId to info) }
    }

    override suspend fun removeDeckRepetitionInfo(deckId: Int) {
        source.update { infos -> infos - deckId }
    }
}

private class DesktopNoOpOldAppKlafDataTransferRepository : IOldAppKlafDataTransferRepository {
    override suspend fun transferOldData() = Unit
}

private class DesktopNoOpCrashlyticsRepository : ICrashlyticsRepository {
    override fun report(exception: Throwable) = Unit
}

private class DesktopAppMaintenanceManager(
    private val mnemonicImageAssetRepository: IMnemonicImageAssetRepository,
) : IAppMaintenanceManager {
    private val state = MutableStateFlow<IDataSynchronizationState>(IDataSynchronizationState.Initial)

    override suspend fun initialize() {
        mnemonicImageAssetRepository.clearAllDraftImages()
    }

    override fun isNetworkConnected(): Boolean = true

    override fun observeDataSynchronizationState(): Flow<IDataSynchronizationState> = state

    override fun performDataSynchronization() {
        state.value = IDataSynchronizationState.SuccessfullyFinished
    }

    override fun scheduleAppReopening() = Unit

    override fun scheduleDeckRepetitionChecking() = Unit
}

private class DesktopNoOpAudioPlayerManager : IAudioPlayerManager {
    override val loadingState = MutableStateFlow<LoadingState<Unit, Unit>>(LoadingState.Non)

    override fun onCreate() = Unit
    override fun onResume() = Unit
    override fun onStop() = Unit
    override fun onDestroy() = Unit
    override fun preparePronunciation(word: String) = Unit
    override fun play() = Unit
    override fun preparePronunciationAndPlay(word: String) = Unit
}

private class DesktopNoOpDeckReviewScheduler : IDeckReviewScheduler {
    override fun schedule(deckName: String, deckId: Int, atTime: Long) = Unit
    override fun cancel(deckId: Int) = Unit
}

private class DesktopNoOpSpeechRecognitionManager : ISpeechRecognitionManager {
    override val isSupported: Boolean = false
    override val state = MutableStateFlow<SpeechRecognitionState>(SpeechRecognitionState.Unsupported)
    override val recognizedText = MutableSharedFlow<SpeechRecognitionResult>()

    override fun startRecognition(targetId: String) = Unit
    override fun stopRecognition() = Unit
    override fun cancelRecognition() = Unit
    override fun release() = Unit
}
