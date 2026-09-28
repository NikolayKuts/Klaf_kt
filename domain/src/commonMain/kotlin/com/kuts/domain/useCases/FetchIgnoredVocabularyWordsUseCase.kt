package com.kuts.domain.useCases

import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.entities.IgnoredVocabularyWord
import com.kuts.domain.repositories.IIgnoredVocabularyWordRepository
import kotlinx.coroutines.withContext

class FetchIgnoredVocabularyWordsUseCase(
    private val ignoredVocabularyWordRepository: IIgnoredVocabularyWordRepository,
    private val coroutineContextProvider: ICoroutineContextProvider,
) {

    suspend operator fun invoke(): List<IgnoredVocabularyWord> = withContext(
        context = coroutineContextProvider.io,
    ) {
        ignoredVocabularyWordRepository.fetchWords()
    }
}
