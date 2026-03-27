package com.kuts.klaf.deckList.drawer

import com.kuts.domain.entities.CodexObserverSessionState
import com.kuts.domain.entities.WordInsightsProvider

data class DrawerViewState(
    val signedIn: Boolean,
    val userEmail: String?,
    val wordInsightsProvider: WordInsightsProvider = WordInsightsProvider.OpenAi,
    val codexObserverSessionState: CodexObserverSessionState = CodexObserverSessionState.Disconnected,
)
