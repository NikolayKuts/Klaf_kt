package com.kuts.klaf.ios

import com.kuts.domain.common.AuthenticationAction
import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.common.IDataSynchronizationState
import com.kuts.domain.common.LoadingState
import com.kuts.domain.entities.AuthenticationState
import com.kuts.domain.entities.AutocompleteWord
import com.kuts.domain.entities.DeckRepetitionInfo
import com.kuts.domain.entities.WordInsightsProvider
import com.kuts.domain.entities.WordInsightsProviderState
import com.kuts.domain.entities.WordInfo
import com.kuts.domain.entities.WordMeaningInsights
import com.kuts.domain.managers.IAppMaintenanceManager
import com.kuts.domain.managers.IAudioPlayerManager
import com.kuts.domain.managers.IAuthenticationSessionManager
import com.kuts.domain.managers.IDeckReviewScheduler
import com.kuts.domain.managers.IWordInsightsProviderManager
import com.kuts.domain.repositories.IAuthenticationRepository
import com.kuts.domain.repositories.ICrashlyticsRepository
import com.kuts.domain.repositories.IDeckRepetitionInfoRepository
import com.kuts.domain.repositories.IOldAppKlafDataTransferRepository
import com.kuts.domain.repositories.IWordAutocompleteRepository
import com.kuts.domain.repositories.IWordInfoRepository
import com.kuts.domain.repositories.IWordMeaningInsightsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlin.coroutines.CoroutineContext

class IosCoroutineContextProvider : ICoroutineContextProvider {
    override val io: CoroutineContext = Dispatchers.Default
}

class IosAuthenticationRepository : IAuthenticationRepository {
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

class IosAuthenticationSessionManager(
    private val authenticationRepository: IosAuthenticationRepository,
) : IAuthenticationSessionManager {

    override fun isSignedIn(): Boolean = authenticationRepository.currentState.email != null
}

class IosNoOpWordInfoRepository : IWordInfoRepository {
    override suspend fun fetchWordInfo(
        word: String,
    ): Flow<LoadingState<WordInfo, IWordInfoRepository.IWordInfoLoadingError>> {
        return flowOf(
            LoadingState.Error(
                value = IWordInfoRepository.IWordInfoLoadingError.Common,
            )
        )
    }
}

class IosNoOpWordAutocompleteRepository : IWordAutocompleteRepository {
    override suspend fun fetchAutocomplete(prefix: String): List<AutocompleteWord> = emptyList()
}

class IosNoOpWordMeaningInsightsRepository : IWordMeaningInsightsRepository {
    override suspend fun fetchWordMeaningInsights(word: String): WordMeaningInsights {
        return WordMeaningInsights.EMPTY
    }
}

class IosNoOpWordInsightsProviderManager : IWordInsightsProviderManager {
    override val state = MutableStateFlow(WordInsightsProviderState())

    override suspend fun setSelectedProvider(provider: WordInsightsProvider) {
        state.value = state.value.copy(selectedProvider = provider)
    }
}

class IosInMemoryDeckRepetitionInfoRepository : IDeckRepetitionInfoRepository {
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

class IosNoOpOldAppKlafDataTransferRepository : IOldAppKlafDataTransferRepository {
    override suspend fun transferOldData() = Unit
}

class IosNoOpCrashlyticsRepository : ICrashlyticsRepository {
    override fun report(exception: Throwable) = Unit
}

class IosAppMaintenanceManager : IAppMaintenanceManager {
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

class IosNoOpAudioPlayerManager : IAudioPlayerManager {
    override val loadingState = MutableStateFlow<LoadingState<Unit, Unit>>(LoadingState.Non)

    override fun onCreate() = Unit
    override fun onResume() = Unit
    override fun onStop() = Unit
    override fun onDestroy() = Unit
    override fun preparePronunciation(word: String) = Unit
    override fun play() = Unit
    override fun preparePronunciationAndPlay(word: String) = Unit
}

class IosNoOpDeckReviewScheduler : IDeckReviewScheduler {
    override fun schedule(deckName: String, deckId: Int, atTime: Long) = Unit
}
