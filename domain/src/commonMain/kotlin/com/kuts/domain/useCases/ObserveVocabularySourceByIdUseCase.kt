package com.kuts.domain.useCases

import com.kuts.domain.entities.VocabularySource
import com.kuts.domain.repositories.IVocabularySourceRepository
import kotlinx.coroutines.flow.Flow

class ObserveVocabularySourceByIdUseCase(
    private val vocabularySourceRepository: IVocabularySourceRepository,
) {

    operator fun invoke(sourceId: Int): Flow<VocabularySource?> {
        return vocabularySourceRepository.observeSourceById(sourceId = sourceId)
    }
}

