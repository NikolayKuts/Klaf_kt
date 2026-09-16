package com.kuts.domain.repositories

import com.kuts.domain.entities.VocabularySourceAnalysis

interface IVocabularySourceAnalysisRepository {

    suspend fun analyze(cleanText: String): VocabularySourceAnalysis
}

