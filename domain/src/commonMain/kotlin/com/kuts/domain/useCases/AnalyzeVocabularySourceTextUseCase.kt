package com.kuts.domain.useCases

import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.entities.VocabularySourceAnalysisResult
import com.kuts.domain.repositories.IVocabularySourceAnalysisRepository
import kotlinx.coroutines.withContext

class AnalyzeVocabularySourceTextUseCase(
    private val vocabularySourceAnalysisRepository: IVocabularySourceAnalysisRepository,
    private val coroutineContextProvider: ICoroutineContextProvider,
) {

    suspend operator fun invoke(
        cleanText: String,
        sourceId: Int? = null,
        sourceTitle: String? = null,
        analysisRetryToken: String? = null,
    ): VocabularySourceAnalysisResult = withContext(
        context = coroutineContextProvider.io,
    ) {
        if (analysisRetryToken != null) {
            vocabularySourceAnalysisRepository.retry(
                cleanText = cleanText,
                analysisRetryToken = analysisRetryToken,
                sourceId = sourceId,
                sourceTitle = sourceTitle,
            )
        } else vocabularySourceAnalysisRepository.analyze(
            cleanText = cleanText,
            sourceId = sourceId,
            sourceTitle = sourceTitle,
        )
    }
}

