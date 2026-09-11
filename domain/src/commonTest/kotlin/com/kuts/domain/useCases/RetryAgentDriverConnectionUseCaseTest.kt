package com.kuts.domain.useCases

import com.kuts.domain.entities.AgentDriverConnectionState
import com.kuts.domain.managers.IAgentDriverConnectionManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class RetryAgentDriverConnectionUseCaseTest {

    @Test
    fun `retry delegates to connection manager on IO context`() = runTest {
        val manager = TestAgentDriverConnectionManager()
        val useCase = RetryAgentDriverConnectionUseCase(
            agentDriverConnectionManager = manager,
            coroutineContextProvider = TestCoroutineContextProvider(
                io = UnconfinedTestDispatcher(testScheduler),
            ),
        )

        useCase()

        assertEquals(expected = 1, actual = manager.retryCount)
    }
}

private class TestAgentDriverConnectionManager : IAgentDriverConnectionManager {

    override val state = MutableStateFlow<AgentDriverConnectionState>(
        value = AgentDriverConnectionState.Disconnected,
    )

    var retryCount = 0
        private set

    override suspend fun retry() {
        retryCount++
    }
}
