package com.kuts.domain.repositories

import com.kuts.domain.entities.VocabularySourceAnalysisResult

interface IVocabularySourceAnalysisRepository {

    suspend fun analyze(
        cleanText: String,
        sourceId: Int? = null,
        sourceTitle: String? = null,
    ): VocabularySourceAnalysisResult
}

