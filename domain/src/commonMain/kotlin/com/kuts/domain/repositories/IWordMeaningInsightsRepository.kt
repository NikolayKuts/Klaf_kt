package com.kuts.domain.repositories

import com.kuts.domain.entities.WordMeaningInsights
import com.kuts.domain.managers.CardLaunchContext

interface IWordMeaningInsightsRepository {

    suspend fun fetchWordMeaningInsights(
        word: String,
        launchContext: CardLaunchContext? = null,
    ): WordMeaningInsights
}
