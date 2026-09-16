package com.kuts.domain.useCases

import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.entities.VocabularySource
import com.kuts.domain.repositories.IStorageSaveVersionRepository
import com.kuts.domain.repositories.IStorageTransactionRepository
import com.kuts.domain.repositories.IVocabularySourceRepository
import kotlinx.coroutines.withContext

class UpdateVocabularySourceUseCase(
    private val vocabularySourceRepository: IVocabularySourceRepository,
    private val localStorageSaveVersionRepository: IStorageSaveVersionRepository,
    private val localStorageTransactionRepository: IStorageTransactionRepository,
    private val coroutineContextProvider: ICoroutineContextProvider,
) {

    suspend operator fun invoke(source: VocabularySource) {
        withContext(context = coroutineContextProvider.io) {
            localStorageTransactionRepository.performWithTransaction {
                vocabularySourceRepository.saveSource(source = source)
                localStorageSaveVersionRepository.increaseVersion()
            }
        }
    }
}

