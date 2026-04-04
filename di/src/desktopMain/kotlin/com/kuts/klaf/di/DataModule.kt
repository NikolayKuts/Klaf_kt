package com.kuts.klaf.di

import com.kuts.domain.common.AuthenticationAction
import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.common.IDataSynchronizationState
import com.kuts.domain.common.LoadingState
import com.kuts.domain.entities.AuthenticationState
import com.kuts.domain.entities.AutocompleteWord
import com.kuts.domain.entities.DeckRepetitionInfo
import com.kuts.domain.entities.WordMeaningInsights
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
import com.kuts.klaf.common.CoroutineContextProvider
import com.kuts.klaf.networking.codexApp.CodexAppWordMeaningInsightsRepository
import com.kuts.klaf.networking.codexApp.DesktopCodexAppHttpClientFactory
import com.kuts.klaf.networking.codexApp.ICodexAppHttpClientFactory
import com.kuts.klaf.networking.openai.OpenAiHttpClientFactory
import com.kuts.klaf.networking.openai.OpenAiWordMeaningInsightsRepository
import com.kuts.klaf.networking.wordInsights.SwitchableWordMeaningInsightsRepository
import com.kuts.klaf.networking.wordInsights.WordInsightsProviderManager
import com.kuts.klaf.networking.yandexApi.YandexSecureHttpClientFactory
import com.kuts.klaf.networking.yandexApi.YandexWordInfoRepository
import com.kuts.klaf.room.databases.KlafRoomDatabase
import com.kuts.klaf.room.databases.KlafRoomDatabaseProvider
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

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
    single<IWordInfoRepository> {
        YandexWordInfoRepository(
            client = YandexSecureHttpClientFactory().create(),
        )
    }
    single<IWordAutocompleteRepository> { DesktopWordAutocompleteRepository() }
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
    single<IDeckRepetitionInfoRepository> { DesktopInMemoryDeckRepetitionInfoRepository() }
    single<IOldAppKlafDataTransferRepository> { DesktopNoOpOldAppKlafDataTransferRepository() }
    single<ICrashlyticsRepository> { DesktopNoOpCrashlyticsRepository() }
}

private fun Module.desktopInfrastructureModule() {
    single<KlafRoomDatabase> { KlafRoomDatabaseProvider.getInstance() }
    single<ICoroutineContextProvider> { CoroutineContextProvider() }
    single<ICodexAppHttpClientFactory> { DesktopCodexAppHttpClientFactory() }
    single<HttpClient>(qualifier = named(name = CODEX_APP_HTTP_CLIENT)) {
        get<ICodexAppHttpClientFactory>().create()
    }
}

private fun Module.desktopManagerBindings() {
    single<IAppMaintenanceManager> { DesktopAppMaintenanceManager() }
    factory<IAudioPlayerManager> { DesktopNoOpAudioPlayerManager() }
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

private class DesktopAppMaintenanceManager : IAppMaintenanceManager {
    private val state = MutableStateFlow<IDataSynchronizationState>(IDataSynchronizationState.Initial)

    override fun initialize() = Unit

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
}
