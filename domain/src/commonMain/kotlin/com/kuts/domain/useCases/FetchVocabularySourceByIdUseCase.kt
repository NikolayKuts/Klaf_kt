package com.kuts.domain.useCases

import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.entities.VocabularySource
import com.kuts.domain.repositories.IVocabularySourceRepository
import kotlinx.coroutines.withContext

class FetchVocabularySourceByIdUseCase(
    private val vocabularySourceRepository: IVocabularySourceRepository,
    private val coroutineContextProvider: ICoroutineContextProvider,
) {

    suspend operator fun invoke(sourceId: Int): VocabularySource? = withContext(
        context = coroutineContextProvider.io,
    ) {
        vocabularySourceRepository.fetchSourceById(sourceId = sourceId)
    }
}

