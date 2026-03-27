package com.kuts.domain.managers

import com.kuts.domain.entities.WordInsightsProvider
import com.kuts.domain.entities.WordInsightsProviderState
import kotlinx.coroutines.flow.StateFlow

interface IWordInsightsProviderManager {

    val state: StateFlow<WordInsightsProviderState>

    suspend fun setSelectedProvider(provider: WordInsightsProvider)
}
