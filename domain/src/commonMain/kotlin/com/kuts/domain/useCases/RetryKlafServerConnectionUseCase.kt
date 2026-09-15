package com.kuts.domain.useCases

import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.managers.IKlafServerConnectionManager
import kotlinx.coroutines.withContext

class RetryKlafServerConnectionUseCase(
    private val klafServerConnectionManager: IKlafServerConnectionManager,
    private val coroutineContextProvider: ICoroutineContextProvider,
) {

    suspend operator fun invoke() = withContext(coroutineContextProvider.io) {
        klafServerConnectionManager.retry()
    }
}
