package com.kuts.domain.useCases

import com.kuts.domain.repositories.IVocabularySourceRepository

class ObserveAllVocabularySourceItemsUseCase(
    private val vocabularySourceRepository: IVocabularySourceRepository,
) {

    operator fun invoke() = vocabularySourceRepository.observeAllItems()
}
