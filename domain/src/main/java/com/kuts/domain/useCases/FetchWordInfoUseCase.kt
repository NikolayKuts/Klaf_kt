package com.kuts.domain.useCases

import com.kuts.domain.common.LoadingState
import com.kuts.domain.entities.WordInfo
import com.kuts.domain.repositories.IWordInfoRepository
import com.kuts.domain.repositories.IWordInfoRepository.IWordInfoLoadingError
import kotlinx.coroutines.flow.Flow

class FetchWordInfoUseCase(
    private val wordInfoRepository: IWordInfoRepository,
) {

    suspend operator fun invoke(word: String): Flow<LoadingState<WordInfo, IWordInfoLoadingError>> {
        return wordInfoRepository.fetchWordInfo(word = word)
    }
}