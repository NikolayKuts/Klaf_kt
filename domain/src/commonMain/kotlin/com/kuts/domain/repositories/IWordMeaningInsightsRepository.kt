package com.kuts.domain.repositories

import com.kuts.domain.entities.WordMeaningInsights

interface IWordMeaningInsightsRepository {

    suspend fun fetchWordMeaningInsights(word: String): WordMeaningInsights
}
