package com.kuts.domain.useCases

import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.entities.WordMeaningInsights
import com.kuts.domain.repositories.IWordMeaningInsightsRepository
import kotlinx.coroutines.withContext

class FetchWordMeaningInsightsUseCase(
    private val wordMeaningInsightsRepository: IWordMeaningInsightsRepository,
    private val coroutineContextProvider: ICoroutineContextProvider,
) {

    suspend operator fun invoke(word: String): WordMeaningInsights = withContext(
        context = coroutineContextProvider.io,
    ) {
        wordMeaningInsightsRepository.fetchWordMeaningInsights(word = word)
    }
}
