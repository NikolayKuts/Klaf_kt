package com.kuts.klaf.cardManagement.cardAddition

import com.kuts.domain.common.LoadingState

data class NativeWordSuggestionsState(
    val suggestions: List<NativeWordSuggestionItem> = emptyList(),
    val isActive: Boolean = false,
    val loadingState: LoadingState<Unit, Unit> = LoadingState.Non,
)