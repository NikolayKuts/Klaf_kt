package com.kuts.domain.useCases

import com.kuts.domain.entities.VocabularySourceItem
import com.kuts.domain.repositories.IVocabularySourceRepository
import kotlinx.coroutines.flow.Flow

class ObserveVocabularySourceItemsUseCase(
    private val vocabularySourceRepository: IVocabularySourceRepository,
) {

    operator fun invoke(sourceId: Int): Flow<List<VocabularySourceItem>> {
        return vocabularySourceRepository.observeItemsBySourceId(sourceId = sourceId)
    }
}

