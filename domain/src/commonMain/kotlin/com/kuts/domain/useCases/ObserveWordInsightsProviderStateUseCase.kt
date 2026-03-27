package com.kuts.domain.useCases

import com.kuts.domain.entities.WordInsightsProviderState
import com.kuts.domain.managers.IWordInsightsProviderManager
import kotlinx.coroutines.flow.StateFlow

class ObserveWordInsightsProviderStateUseCase(
    private val wordInsightsProviderManager: IWordInsightsProviderManager,
) {

    operator fun invoke(): StateFlow<WordInsightsProviderState> {
        return wordInsightsProviderManager.state
    }
}
