package com.kuts.klaf.networking.klafServer

import com.kuts.domain.entities.KlafServerConnectionState
import com.kuts.domain.entities.VocabularyAnalysisRetryAvailableException
import com.kuts.domain.entities.VocabularyAnalysisRetryExpiredException
import com.kuts.klaf.server.contract.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking

private class RetryTestSession : IKlafServerSession {
    override val connectionState = MutableStateFlow<KlafServerConnectionState>(KlafServerConnectionState.Disconnected)
    override val clientSessionId = "test"
    var response: KlafServerMessage = KlafServerErrorMessage("r", KlafServerErrorCode.ASSISTANT_FAILED,
        "Try again", analysisRetryToken = "checkpoint-token")
    var lastRequest: VocabularySourceAnalyzeRequest? = null
    override suspend fun request(message: KlafServerClientMessage): KlafServerMessage {
        lastRequest = message as VocabularySourceAnalyzeRequest
        return response
    }
    override suspend fun connect() = Unit
    override suspend fun disconnect() = Unit
    override suspend fun cancelRequest(requestId: String) = Unit
    override suspend fun nextRequestId(prefix: String) = "r"
    override fun transcribeAudio(
        requestId: String,
        sourceId: Int?,
        sourceTitle: String?,
        fileName: String,
        audioFormat: String,
        declaredByteSize: Long,
        audioStreamProvider: suspend (suspend (ByteArray) -> Unit) -> Unit,
    ): Flow<VocabularySourceTranscriptionSessionEvent> = error("Unused")
}

class VocabularyAnalysisRetryRepositoryTest {
    @Test
    fun failureExposesOpaqueRetryAndRetrySendsItWithExactSourceAndText() = runBlocking<Unit> {
        val session = RetryTestSession()
        val repository = KlafServerVocabularySourceAnalysisRepository(session)
        val failure = assertFailsWith<VocabularyAnalysisRetryAvailableException> {
            repository.analyze("A science project.", 7, "Source")
        }
        assertEquals("checkpoint-token", failure.retryToken)
        assertEquals(null, session.lastRequest?.analysisRetryToken)
        session.response = VocabularySourceAnalyzedMessage("r", VocabularySourceAnalysisDto("en", emptyList()))
        repository.retry("A science project.", failure.retryToken, 7, "Source")
        assertEquals("checkpoint-token", session.lastRequest?.analysisRetryToken)
        assertEquals(7, session.lastRequest?.sourceId)
        assertEquals("A science project.", session.lastRequest?.cleanText)
    }

    @Test
    fun expiredCheckpointRequiresNewAnalyze() = runBlocking<Unit> {
        val session = RetryTestSession().apply {
            response = KlafServerErrorMessage("r", KlafServerErrorCode.VALIDATION_FAILED, "Start new analysis")
        }
        assertFailsWith<VocabularyAnalysisRetryExpiredException> {
            KlafServerVocabularySourceAnalysisRepository(session).retry("Original text", "expired", 7, "Source")
        }
    }
}
