package com.kuts.domain.useCases

import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.common.LoadingState
import com.kuts.domain.entities.WordInfo
import com.kuts.domain.repositories.IWordInfoRepository
import com.kuts.domain.repositories.IWordInfoRepository.IWordInfoLoadingError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class FetchWordInfoUseCase(
    private val wordInfoRepository: IWordInfoRepository,
    private val coroutineContextProvider: ICoroutineContextProvider,
) {

    suspend operator fun invoke(word: String): Flow<LoadingState<WordInfo, IWordInfoLoadingError>> {
        return withContext(context = coroutineContextProvider.io) {
            wordInfoRepository.fetchWordInfo(word = word)
        }
    }
}
