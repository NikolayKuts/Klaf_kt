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
import com.kuts.klaf.common.WordMeaningInsightsPayload
import com.kuts.klaf.common.toDomainEntity
import com.kuts.klaf.networking.agentDriver.AgentDriverSession
import com.kuts.klaf.networking.agentDriver.describeChainForLog
import com.kuts.klaf.networking.agentDriver.isFault
import com.kuts.klaf.networking.agentDriver.toShortAgentDriverMessage
import com.lib.lokdroid.core.logD
import com.lib.lokdroid.core.logE
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
import kotlinx.serialization.json.Json
import org.agentdriver.project.ktorclient.external.AssistantClientConnectionState
import org.agentdriver.project.ktorclient.external.AssistantClientDisconnectCause
import org.agentdriver.project.protocol.TextGenerationRequest

/**
 * Owns the provider switch, and reports how the assistant connection is doing.
 *
 * The switch decides more than who answers word-meaning questions: while it is on OpenAI there is
 * no connection to the assistant at all, so the features that only the assistant can serve --
 * mnemonic associations and their illustrations -- are unavailable too.
 */
class WordInsightsProviderManager(
    private val dataStore: DataStore<Preferences>,
    private val agentDriverSession: AgentDriverSession,
    coroutineContextProvider: ICoroutineContextProvider,
) : IWordInsightsProviderManager {

    companion object {

        private const val KEY_SELECTED_PROVIDER = "word_insights_selected_provider"
        private const val PROVIDER_OPEN_AI = "open_ai"
        private const val PROVIDER_CODEX_OBSERVER = "codex_observer"
    }

    override val state = MutableStateFlow(WordInsightsProviderState())

    @Suppress("OPT_IN_USAGE")
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private val scope = CoroutineScope(coroutineContextProvider.io + SupervisorJob())
    private val selectedProviderKey = stringPreferencesKey(KEY_SELECTED_PROVIDER)
    private val initialSelectionLoaded = CompletableDeferred<Unit>()

    init {
        observeSelectedProvider()
        observeAgentDriverConnection()
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
        require(value = state.value.selectedProvider == WordInsightsProvider.CodexObserver) {
            "CodeX Observer is disabled."
        }

        val requestedWord = word.trim()
        require(value = requestedWord.isNotBlank()) { "Word must not be blank." }

        val prompt = WordMeaningInsightsPromptFactory.build(word = requestedWord)

        val rawResponse = try {
            agentDriverSession.generateText(
                request = TextGenerationRequest(
                    prompt = prompt.agentDriverPrompt,
                    responseSchema = prompt.responseSchema,
                ),
            ).also { response ->
                logD("Word insights for \"$requestedWord\": ${response.length} characters")
            }
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (throwable: Throwable) {
            // The typed cause, not just the shortened one shown to the user: a failure here is
            // either the connection or the request, and only the type says which.
            logE("Word insights request failed: ${throwable::class.simpleName} -- $throwable")
            throw IllegalArgumentException(
                "CodeX Observer request failed. ${throwable.toShortAgentDriverMessage()}",
                throwable,
            )
        }

        // Plain JSON, no code fence to strip: the request carried a response schema, and an answer
        // to one arrives as the object it describes.
        val payload = json.decodeFromString(
            deserializer = WordMeaningInsightsPayload.serializer(),
            string = rawResponse,
        )
        payload.throwIfInvalidForRequestedWord(requestedWord = requestedWord)

        val parsedInsights = payload.toDomainEntity()
        require(value = parsedInsights.isValidForContract(expectedWord = requestedWord)) {
            "The assistant returned a payload that does not match the expected contract."
        }

        return parsedInsights
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
                    state.value = state.value.copy(selectedProvider = provider)
                    markInitialSelectionLoaded()

                    // The switch is the only thing that opens or closes the connection. A failure
                    // to open is not fatal: it shows up in the state, and the SDK keeps trying.
                    when (provider) {
                        WordInsightsProvider.CodexObserver -> {
                            logD("Assistant switched on")
                            applySwitch { agentDriverSession.switchOn() }
                        }

                        WordInsightsProvider.OpenAi -> {
                            logD("Assistant switched off")
                            applySwitch { agentDriverSession.switchOff() }
                        }
                    }
                }
        }
    }

    /**
     * Runs one side of the switch, tolerating a failure but not a cancellation.
     *
     * `runCatching` alone would swallow the cancellation `collectLatest` throws when the switch is
     * flipped again mid-connect, which would let a coroutine that is supposed to be gone carry on.
     */
    private suspend fun applySwitch(apply: suspend () -> Unit) {
        try {
            apply()
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (throwable: Throwable) {
            logE("Applying the assistant switch failed: ${throwable.describeChainForLog()}")
        }
    }

    private fun observeAgentDriverConnection() {
        scope.launch {
            agentDriverSession.connectionState.collectLatest { connectionState ->
                // Every transition, not just the ones a user action caused. The SDK reconnects on
                // its own schedule, so without this a connection that drops and comes back leaves
                // no trace at all -- and one that never comes back leaves nothing to read either.
                logD("AgentDriver connection: ${connectionState.describeForLog()}")

                state.value = state.value.copy(
                    codexObserverSessionState = connectionState.toCodexObserverSessionState(),
                )
            }
        }
    }

    /**
     * The connection state with its reason attached.
     *
     * The SDK says why it is not connected, not merely that it is not; printing the state alone
     * would throw away the part that says what to fix.
     */
    private fun AssistantClientConnectionState.describeForLog(): String = when (this) {
        is AssistantClientConnectionState.Connected ->
            "Connected (provider=${session.provider}, model=${session.modelId.value}, " +
                "capabilities=${session.capabilities.map { it.wireName }})"

        is AssistantClientConnectionState.Connecting ->
            "Connecting (attempt=$attempt, resuming=$resuming)"

        is AssistantClientConnectionState.ResumeAvailable -> "ResumeAvailable, cause=$cause"

        is AssistantClientConnectionState.Disconnected ->
            "Disconnected, cause=${cause.describeForLog()}, willRetry=${cause.allowsAutomaticReconnect}"
    }

    /**
     * A disconnect cause with the whole chain behind it.
     *
     * `TransportFailure` alone says only that the socket did not open; what actually went wrong --
     * a name that would not resolve, a handshake the server refused, a certificate the device would
     * not trust -- is in the exception it wraps, sometimes several levels down.
     */
    private fun AssistantClientDisconnectCause.describeForLog(): String = when (this) {
        is AssistantClientDisconnectCause.OpenFailed -> "OpenFailed(${failure.describeChainForLog()})"
        else -> toString()
    }

    /**
     * The connection state as the drawer shows it.
     *
     * A drop is reported as a drop, with its reason, whether or not the SDK intends to reconnect
     * from it. Reporting a recoverable drop as "connecting" hides the one thing the user needs --
     * a server that is not running, a gateway answering 502, a certificate the device refuses --
     * behind a status that never changes and never explains itself.
     */
    private fun AssistantClientConnectionState.toCodexObserverSessionState(): CodexObserverSessionState {
        return when (this) {
            is AssistantClientConnectionState.Connected -> CodexObserverSessionState.Ready
            is AssistantClientConnectionState.Connecting -> CodexObserverSessionState.Connecting
            is AssistantClientConnectionState.ResumeAvailable -> cause.toCodexObserverSessionState()
            is AssistantClientConnectionState.Disconnected -> cause.toCodexObserverSessionState()
        }
    }

    private fun AssistantClientDisconnectCause.toCodexObserverSessionState(): CodexObserverSessionState {
        return when {
            isFault -> CodexObserverSessionState.Error(message = toShortAgentDriverMessage())

            // Not connected on purpose -- never started, or the user switched the assistant off.
            else -> CodexObserverSessionState.Disconnected
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
