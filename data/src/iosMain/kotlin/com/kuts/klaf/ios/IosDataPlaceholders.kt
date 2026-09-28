package com.kuts.klaf.ios

import com.kuts.domain.common.AuthenticationAction
import com.kuts.domain.common.LoadingState
import com.kuts.domain.entities.KlafServerConnectionState
import com.kuts.domain.entities.AuthenticationState
import com.kuts.domain.entities.AutocompleteWord
import com.kuts.domain.entities.WordMeaningInsights
import com.kuts.domain.managers.CardLaunchContext
import com.kuts.domain.managers.IAudioPlayerManager
import com.kuts.domain.managers.IKlafServerConnectionManager
import com.kuts.domain.managers.IAuthenticationSessionManager
import com.kuts.domain.managers.IDeckReviewScheduler
import com.kuts.domain.managers.ITextToSpeechManager
import com.kuts.domain.repositories.IAuthenticationRepository
import com.kuts.domain.repositories.ICrashlyticsRepository
import com.kuts.domain.repositories.IOldAppKlafDataTransferRepository
import com.kuts.domain.repositories.IWordAutocompleteRepository
import com.kuts.domain.repositories.IWordMeaningInsightsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

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

class IosNoOpWordAutocompleteRepository : IWordAutocompleteRepository {
    override val isEnabled: Boolean = false

    override suspend fun fetchAutocomplete(prefix: String): List<AutocompleteWord> = emptyList()
}

class IosNoOpWordMeaningInsightsRepository : IWordMeaningInsightsRepository {
    override suspend fun fetchWordMeaningInsights(
        word: String,
        launchContext: CardLaunchContext?,
    ): WordMeaningInsights {
        error("Word insights through Klaf Server are not implemented on iOS yet.")
    }
}

class IosNoOpKlafServerConnectionManager : IKlafServerConnectionManager {
    override val state = MutableStateFlow<KlafServerConnectionState>(
        value = KlafServerConnectionState.Disconnected,
    )

    override suspend fun retry() = Unit
}

class IosNoOpOldAppKlafDataTransferRepository : IOldAppKlafDataTransferRepository {
    override suspend fun transferOldData() = Unit
}

class IosNoOpCrashlyticsRepository : ICrashlyticsRepository {
    override fun report(exception: Throwable) = Unit
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

class IosNoOpTextToSpeechManager : ITextToSpeechManager {
    override fun speak(text: String) = Unit
    override fun stop() = Unit
    override fun shutdown() = Unit
}

class IosNoOpDeckReviewScheduler : IDeckReviewScheduler {
    override fun schedule(deckName: String, deckId: Int, atTime: Long) = Unit
    override fun cancel(deckId: Int) = Unit
}
