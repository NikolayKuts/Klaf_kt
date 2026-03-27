package com.kuts.domain.useCases

import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.entities.WordInsightsProvider
import com.kuts.domain.managers.IWordInsightsProviderManager
import kotlinx.coroutines.withContext

class SetWordInsightsProviderUseCase(
    private val wordInsightsProviderManager: IWordInsightsProviderManager,
    private val coroutineContextProvider: ICoroutineContextProvider,
) {

    suspend operator fun invoke(provider: WordInsightsProvider) = withContext(
        context = coroutineContextProvider.io,
    ) {
        wordInsightsProviderManager.setSelectedProvider(provider = provider)
    }
}
