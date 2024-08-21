package com.kuts.domain.useCases

import com.kuts.domain.common.LoadingState
import com.kuts.domain.entities.WordInfo
import com.kuts.domain.repositories.WordInfoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FetchWordInfoUseCase @Inject constructor(
    private val wordInfoRepository: WordInfoRepository,
) {

    suspend operator fun invoke(word: String): Flow<LoadingState<WordInfo>> {
        return wordInfoRepository.fetchWordInfo(word = word)
    }
}