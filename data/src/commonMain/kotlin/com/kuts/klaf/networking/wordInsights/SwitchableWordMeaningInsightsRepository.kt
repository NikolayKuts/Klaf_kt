package com.kuts.klaf.networking.wordInsights

import com.kuts.domain.entities.WordInsightsProvider
import com.kuts.domain.entities.WordMeaningInsights
import com.kuts.domain.repositories.IWordMeaningInsightsRepository
import com.kuts.klaf.networking.agentDriver.AgentDriverWordMeaningInsightsRepository
import com.kuts.klaf.networking.openai.OpenAiWordMeaningInsightsRepository

class SwitchableWordMeaningInsightsRepository(
    private val manager: WordInsightsProviderManager,
    private val openAiRepository: OpenAiWordMeaningInsightsRepository,
    private val agentDriverRepository: AgentDriverWordMeaningInsightsRepository,
) : IWordMeaningInsightsRepository {

    override suspend fun fetchWordMeaningInsights(word: String): WordMeaningInsights {
        return when (manager.awaitSelectedProvider()) {
            WordInsightsProvider.OpenAi -> openAiRepository.fetchWordMeaningInsights(word = word)

            WordInsightsProvider.CodexObserver ->
                agentDriverRepository.fetchWordMeaningInsights(word = word)
        }
    }
}
