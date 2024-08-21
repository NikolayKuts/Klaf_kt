package com.kuts.domain.repositories

import com.kuts.domain.common.LoadingState
import com.kuts.domain.entities.WordInfo
import kotlinx.coroutines.flow.Flow

interface WordInfoRepository {

    suspend fun fetchWordInfo(word: String): Flow<LoadingState<WordInfo>>
}