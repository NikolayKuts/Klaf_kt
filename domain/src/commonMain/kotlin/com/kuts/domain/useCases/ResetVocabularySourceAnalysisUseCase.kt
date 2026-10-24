package com.kuts.domain.useCases

import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.repositories.IStorageSaveVersionRepository
import com.kuts.domain.repositories.IStorageTransactionRepository
import com.kuts.domain.repositories.IVocabularySourceRepository
import kotlinx.coroutines.withContext

class ResetVocabularySourceAnalysisUseCase(
    private val vocabularySourceRepository: IVocabularySourceRepository,
    private val localStorageSaveVersionRepository: IStorageSaveVersionRepository,
    private val localStorageTransactionRepository: IStorageTransactionRepository,
    private val coroutineContextProvider: ICoroutineContextProvider,
) {
    suspend operator fun invoke(sourceId: Int) = withContext(coroutineContextProvider.io) {
        localStorageTransactionRepository.performWithTransaction {
            requireNotNull(vocabularySourceRepository.fetchSourceById(sourceId)) { "Source no longer exists" }
            vocabularySourceRepository.removeItemsBySourceId(sourceId)
            localStorageSaveVersionRepository.increaseVersion()
        }
    }
}
