package com.kuts.domain.useCases

import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.entities.VocabularySourceItem
import com.kuts.domain.repositories.IVocabularySourceRepository
import kotlinx.coroutines.withContext

class FetchVocabularySourceItemsUseCase(
    private val vocabularySourceRepository: IVocabularySourceRepository,
    private val coroutineContextProvider: ICoroutineContextProvider,
) {

    suspend operator fun invoke(sourceId: Int): List<VocabularySourceItem> = withContext(
        context = coroutineContextProvider.io,
    ) {
        vocabularySourceRepository.fetchItemsBySourceId(sourceId = sourceId)
    }
}

