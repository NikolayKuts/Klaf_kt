package com.kuts.klaf.networking.codexApp

import com.kuts.domain.entities.WordMeaningInsights
import com.kuts.domain.repositories.IWordMeaningInsightsRepository
import com.kuts.klaf.networking.wordInsights.WordInsightsProviderManager

class CodexAppWordMeaningInsightsRepository(
    private val manager: WordInsightsProviderManager,
) : IWordMeaningInsightsRepository {

    override suspend fun fetchWordMeaningInsights(word: String): WordMeaningInsights {
        return manager.fetchCodexWordMeaningInsights(word = word)
    }
}
