package com.kuts.domain.repositories

import com.kuts.domain.entities.VocabularySourceAnalysisResult

interface IVocabularySourceAnalysisRepository {

    suspend fun retry(
        cleanText: String,
        analysisRetryToken: String,
        sourceId: Int? = null,
        sourceTitle: String? = null,
    ): VocabularySourceAnalysisResult = error("Analysis retry is unavailable.")

    suspend fun analyze(
        cleanText: String,
        sourceId: Int? = null,
        sourceTitle: String? = null,
    ): VocabularySourceAnalysisResult
}

