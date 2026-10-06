package com.kuts.klaf.networking.klafServer

import com.kuts.domain.entities.KlafServerConnectionState
import com.kuts.klaf.common.CoroutineContextProvider
import com.kuts.klaf.server.contract.KlafServerClientMessage
import com.kuts.klaf.server.contract.KlafServerMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals

class KlafServerConnectionManagerTest {

    @Test
    fun signInReconnectsAfterUnauthenticatedStartup() = runBlocking {
        val selectedAccount = MutableStateFlow<String?>(null)
        val session = RecordingSession()
        KlafServerConnectionManager(session, CoroutineContextProvider(), selectedAccount)

        delay(100)
        assertEquals(0, session.connectCount)

        session.connectionState.value = KlafServerConnectionState.Error("prior authentication failure")
        selectedAccount.value = "account@example.test"
        withTimeout(2_000) {
            while (session.connectCount < 1) delay(10)
        }
        assertEquals(KlafServerConnectionState.Ready, session.connectionState.value)
    }

    @Test
    fun accountSwitchReconnectsOnlyForSelectedAccount() = runBlocking {
        val selectedAccount = MutableStateFlow<String?>("first@example.test")
        val session = RecordingSession()
        KlafServerConnectionManager(session, CoroutineContextProvider(), selectedAccount)

        withTimeout(2_000) {
            while (session.connectCount < 1) delay(10)
        }
        selectedAccount.value = null
        delay(100)
        assertEquals(1, session.connectCount)

        selectedAccount.value = "second@example.test"
        withTimeout(2_000) {
            while (session.connectCount < 2) delay(10)
        }
        assertEquals(2, session.connectCount)
    }

    @Test
    fun sameAccountSignInClosesOldAiSessionAndReconnects() = runBlocking {
        val selectedAccount = MutableStateFlow<String?>("account@example.test")
        val sameAccountSignIns = MutableStateFlow(0L)
        val session = RecordingSession()
        KlafServerConnectionManager(
            session, CoroutineContextProvider(), selectedAccount, sameAccountSignIns,
        )

        withTimeout(2_000) {
            while (session.connectCount < 1) delay(10)
        }
        sameAccountSignIns.value = 1L
        withTimeout(2_000) {
            while (session.connectCount < 2) delay(10)
        }
        assertEquals(1, session.endUserSessionCount)
    }

    private class RecordingSession : IKlafServerSession {
        override val connectionState: MutableStateFlow<KlafServerConnectionState> =
            MutableStateFlow(KlafServerConnectionState.Disconnected)
        override val clientSessionId: String = "test"

        @Volatile
        var connectCount: Int = 0
            private set
        @Volatile
        var endUserSessionCount: Int = 0
            private set

        override suspend fun connect() {
            connectCount++
            connectionState.value = KlafServerConnectionState.Ready
        }

        override suspend fun request(message: KlafServerClientMessage): KlafServerMessage = error("unused")

        override fun transcribeAudio(
            requestId: String,
            sourceId: Int?,
            sourceTitle: String?,
            fileName: String,
            audioFormat: String,
            declaredByteSize: Long,
            audioStreamProvider: suspend (sendChunk: suspend (ByteArray) -> Unit) -> Unit,
        ): Flow<VocabularySourceTranscriptionSessionEvent> = emptyFlow()

        override suspend fun cancelRequest(requestId: String) = Unit

        override suspend fun nextRequestId(prefix: String): String = "$prefix-test-0"

        override suspend fun disconnect() {
            connectionState.value = KlafServerConnectionState.Disconnected
        }

        override suspend fun endUserSession() {
            endUserSessionCount++
            disconnect()
        }
    }
}
