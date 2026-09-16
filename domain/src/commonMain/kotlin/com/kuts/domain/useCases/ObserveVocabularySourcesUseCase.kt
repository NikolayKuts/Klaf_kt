package com.kuts.domain.useCases

import com.kuts.domain.entities.VocabularySource
import com.kuts.domain.repositories.IVocabularySourceRepository
import kotlinx.coroutines.flow.Flow

class ObserveVocabularySourcesUseCase(
    private val vocabularySourceRepository: IVocabularySourceRepository,
) {

    operator fun invoke(): Flow<List<VocabularySource>> {
        return vocabularySourceRepository.observeSources()
    }
}

