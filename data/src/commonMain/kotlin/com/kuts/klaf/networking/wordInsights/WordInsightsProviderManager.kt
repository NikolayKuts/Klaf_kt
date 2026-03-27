package com.kuts.klaf.networking.wordInsights

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.entities.CodexObserverSessionState
import com.kuts.domain.entities.WordInsightsProvider
import com.kuts.domain.entities.WordInsightsProviderState
import com.kuts.domain.entities.WordMeaningInsights
import com.kuts.domain.managers.IWordInsightsProviderManager
import io.ktor.client.HttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class WordInsightsProviderManager(
    private val dataStore: DataStore<Preferences>,
    private val codexClient: HttpClient,
    coroutineContextProvider: ICoroutineContextProvider,
    private val codexServerUrl: String,
    private val codexModel: String? = null,
) : IWordInsightsProviderManager {

    companion object {

        private const val KEY_SELECTED_PROVIDER = "word_insights_selected_provider"
        private const val PROVIDER_OPEN_AI = "open_ai"
        private const val PROVIDER_CODEX_OBSERVER = "codex_observer"
    }

    override val state = MutableStateFlow(WordInsightsProviderState())

    private val scope = CoroutineScope(coroutineContextProvider.io + SupervisorJob())
    private val sessionMutex = Mutex()
    private val selectedProviderKey = stringPreferencesKey(KEY_SELECTED_PROVIDER)
    private val initialSelectionLoaded = CompletableDeferred<Unit>()
    private var activeCodexSession: CodexObserverHotSession? = null

    init {
        observeSelectedProvider()
    }

    override suspend fun setSelectedProvider(provider: WordInsightsProvider) {
        dataStore.edit { preferences ->
            preferences[selectedProviderKey] = provider.toStoredValue()
        }
    }

    internal suspend fun awaitSelectedProvider(): WordInsightsProvider {
        initialSelectionLoaded.await()
        return state.value.selectedProvider
    }

    internal suspend fun fetchCodexWordMeaningInsights(word: String): WordMeaningInsights {
        initialSelectionLoaded.await()

        return sessionMutex.withLock {
            val selectedProvider = state.value.selectedProvider
            require(value = selectedProvider == WordInsightsProvider.CodexObserver) {
                "CodeX Observer is disabled."
            }

            val activeSession = activeCodexSession
            if (activeSession == null) {
                throw IllegalArgumentException(buildCodexUnavailableMessage())
            }

            try {
                activeSession.fetchWordMeaningInsights(word = word)
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (throwable: Throwable) {
                handleCodexSessionFailureLocked(throwable = throwable)
                throw IllegalArgumentException(
                    "CodeX Observer request failed. ${throwable.toShortCodexMessage()}",
                    throwable,
                )
            }
        }
    }

    private fun observeSelectedProvider() {
        scope.launch {
            dataStore.data
                .catch { emit(emptyPreferences()) }
                .map { preferences ->
                    preferences[selectedProviderKey]
                        ?.toWordInsightsProvider()
                        ?: WordInsightsProvider.OpenAi
                }
                .distinctUntilChanged()
                .collectLatest { provider ->
                    when (provider) {
                        WordInsightsProvider.OpenAi -> activateOpenAi()
                        WordInsightsProvider.CodexObserver -> activateCodexObserver()
                    }
                }
        }
    }

    private suspend fun activateOpenAi() {
        sessionMutex.withLock {
            closeCodexSessionLocked(reasonMessage = "word insights provider switched to OpenAI")
            state.value = WordInsightsProviderState(
                selectedProvider = WordInsightsProvider.OpenAi,
                codexObserverSessionState = CodexObserverSessionState.Disconnected,
            )
            markInitialSelectionLoaded()
        }
    }

    private suspend fun activateCodexObserver() {
        sessionMutex.withLock {
            closeCodexSessionLocked(reasonMessage = "restarting CodeX Observer session")
            state.value = WordInsightsProviderState(
                selectedProvider = WordInsightsProvider.CodexObserver,
                codexObserverSessionState = CodexObserverSessionState.Connecting,
            )
            markInitialSelectionLoaded()

            try {
                activeCodexSession = CodexObserverHotSession.connect(
                    client = codexClient,
                    serverUrl = codexServerUrl,
                    model = codexModel,
                )
                state.value = state.value.copy(
                    codexObserverSessionState = CodexObserverSessionState.Ready,
                )
            } catch (cancellationException: CancellationException) {
                closeCodexSessionLocked(reasonMessage = "CodeX Observer connection cancelled")
                throw cancellationException
            } catch (throwable: Throwable) {
                handleCodexSessionFailureLocked(throwable = throwable)
            }
        }
    }

    private suspend fun closeCodexSessionLocked(reasonMessage: String) {
        val currentSession = activeCodexSession ?: return
        activeCodexSession = null
        try {
            currentSession.close(reasonMessage = reasonMessage)
        } catch (_: Throwable) {
            // Ignore session close errors. The next state update is authoritative.
        }
    }

    private suspend fun handleCodexSessionFailureLocked(throwable: Throwable) {
        closeCodexSessionLocked(reasonMessage = "CodeX Observer session failed")
        state.value = WordInsightsProviderState(
            selectedProvider = WordInsightsProvider.CodexObserver,
            codexObserverSessionState = CodexObserverSessionState.Error(
                message = throwable.toShortCodexMessage(),
            ),
        )
    }

    private fun buildCodexUnavailableMessage(): String {
        return when (val codexState = state.value.codexObserverSessionState) {
            CodexObserverSessionState.Disconnected -> "CodeX Observer session is not ready."
            CodexObserverSessionState.Connecting -> "CodeX Observer is still connecting."
            CodexObserverSessionState.Ready -> "CodeX Observer session is not ready."
            is CodexObserverSessionState.Error -> "CodeX Observer is unavailable. ${codexState.message}"
        }
    }

    private fun markInitialSelectionLoaded() {
        if (!initialSelectionLoaded.isCompleted) {
            initialSelectionLoaded.complete(Unit)
        }
    }

    private fun WordInsightsProvider.toStoredValue(): String {
        return when (this) {
            WordInsightsProvider.OpenAi -> PROVIDER_OPEN_AI
            WordInsightsProvider.CodexObserver -> PROVIDER_CODEX_OBSERVER
        }
    }

    private fun String.toWordInsightsProvider(): WordInsightsProvider? {
        return when (this) {
            PROVIDER_OPEN_AI -> WordInsightsProvider.OpenAi
            PROVIDER_CODEX_OBSERVER -> WordInsightsProvider.CodexObserver
            else -> null
        }
    }
}

private fun Throwable.toShortCodexMessage(): String {
    val rawMessage = message
        ?.lineSequence()
        ?.firstOrNull()
        ?.trim()
        .orEmpty()

    return when {
        rawMessage.contains(other = "not authenticated", ignoreCase = true) -> "Not authenticated"
        rawMessage.contains(other = "timed out", ignoreCase = true) -> "Timeout"
        rawMessage.contains(other = "unknown host", ignoreCase = true) -> "Unknown host"
        rawMessage.contains(other = "resolve host", ignoreCase = true) -> "Host unreachable"
        rawMessage.contains(other = "connection refused", ignoreCase = true) -> "Connection refused"
        rawMessage.contains(other = "closed unexpectedly", ignoreCase = true) -> "Connection closed"
        rawMessage.contains(other = "failed to generate insights", ignoreCase = true) -> {
            rawMessage
                .substringAfter(delimiter = "failed to generate insights.", missingDelimiterValue = rawMessage)
                .trim()
                .ifBlank { "Generation failed" }
                .take(n = 80)
        }

        rawMessage.isBlank() -> "Connection failed"
        else -> rawMessage.substringBefore(delimiter = ". ").take(n = 80)
    }
}
