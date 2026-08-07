package com.kuts.klaf.networking.agentDriver

import com.kuts.domain.entities.WordMeaningInsights
import com.kuts.domain.repositories.IWordMeaningInsightsRepository
import com.kuts.klaf.networking.wordInsights.WordInsightsProviderManager

class AgentDriverWordMeaningInsightsRepository(
    private val manager: WordInsightsProviderManager,
) : IWordMeaningInsightsRepository {

    override suspend fun fetchWordMeaningInsights(word: String): WordMeaningInsights {
        return manager.fetchCodexWordMeaningInsights(word = word)
    }
}
