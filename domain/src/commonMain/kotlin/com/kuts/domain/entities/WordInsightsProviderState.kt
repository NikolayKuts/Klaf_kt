package com.kuts.domain.entities

enum class WordInsightsProvider {
    OpenAi,
    CodexObserver,
}

sealed interface CodexObserverSessionState {

    object Disconnected : CodexObserverSessionState
    object Connecting : CodexObserverSessionState
    object Ready : CodexObserverSessionState
    data class Error(val message: String) : CodexObserverSessionState
}

data class WordInsightsProviderState(
    val selectedProvider: WordInsightsProvider = WordInsightsProvider.OpenAi,
    val codexObserverSessionState: CodexObserverSessionState = CodexObserverSessionState.Disconnected,
)
