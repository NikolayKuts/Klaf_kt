package com.kuts.domain.useCases

import com.kuts.domain.common.ICoroutineContextProvider
import com.kuts.domain.managers.IAgentDriverConnectionManager
import kotlinx.coroutines.withContext

class RetryAgentDriverConnectionUseCase(
    private val agentDriverConnectionManager: IAgentDriverConnectionManager,
    private val coroutineContextProvider: ICoroutineContextProvider,
) {

    suspend operator fun invoke() = withContext(coroutineContextProvider.io) {
        agentDriverConnectionManager.retry()
    }
}
