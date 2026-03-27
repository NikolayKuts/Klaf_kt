package com.kuts.klaf.networking.wordInsights

import com.kuts.domain.entities.WordInsightsProvider
import com.kuts.domain.entities.WordMeaningInsights
import com.kuts.domain.repositories.IWordMeaningInsightsRepository
import com.kuts.klaf.networking.codexApp.CodexAppWordMeaningInsightsRepository
import com.kuts.klaf.networking.openai.OpenAiWordMeaningInsightsRepository

class SwitchableWordMeaningInsightsRepository(
    private val manager: WordInsightsProviderManager,
    private val openAiRepository: OpenAiWordMeaningInsightsRepository,
    private val codexRepository: CodexAppWordMeaningInsightsRepository,
) : IWordMeaningInsightsRepository {

    override suspend fun fetchWordMeaningInsights(word: String): WordMeaningInsights {
        val selectedProvider = manager.awaitSelectedProvider()

        return try {
            val result = when (selectedProvider) {
                WordInsightsProvider.OpenAi -> openAiRepository.fetchWordMeaningInsights(word = word)
                WordInsightsProvider.CodexObserver -> codexRepository.fetchWordMeaningInsights(word = word)
            }

            result
        } catch (throwable: Throwable) {
            throw throwable
        }
    }
}
