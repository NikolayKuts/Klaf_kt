package com.kuts.domain.useCases

import com.kuts.domain.entities.KlafServerConnectionState
import com.kuts.domain.managers.IKlafServerConnectionManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class RetryKlafServerConnectionUseCaseTest {

    @Test
    fun `retry delegates to connection manager on IO context`() = runTest {
        val manager = TestKlafServerConnectionManager()
        val useCase = RetryKlafServerConnectionUseCase(
            klafServerConnectionManager = manager,
            coroutineContextProvider = TestCoroutineContextProvider(
                io = UnconfinedTestDispatcher(testScheduler),
            ),
        )

        useCase()

        assertEquals(expected = 1, actual = manager.retryCount)
    }
}

private class TestKlafServerConnectionManager : IKlafServerConnectionManager {

    override val state = MutableStateFlow<KlafServerConnectionState>(
        value = KlafServerConnectionState.Disconnected,
    )

    var retryCount = 0
        private set

    override suspend fun retry() {
        retryCount++
    }
}
