package com.kuts.domain.vocabularySource

import com.kuts.domain.managers.IVocabularySourceTranscriptionBackgroundManager
import com.kuts.domain.managers.VocabularySourceTranscriptionCoordinator
import com.kuts.domain.managers.VocabularySourceTranscriptionHandle
import com.kuts.domain.managers.VocabularySourceTranscriptionOutcome
import com.kuts.domain.repositories.IVocabularySourceTranscriptionRepository
import com.kuts.domain.repositories.VocabularySourceTranscriptionResult
import com.kuts.domain.repositories.VocabularySourceTranscriptionUpdate
import com.kuts.domain.useCases.TranscribeVocabularySourceAudioUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class CoordinatorTestRepository : IVocabularySourceTranscriptionRepository {
    val result = CompletableDeferred<VocabularySourceTranscriptionUpdate.Success>()
    var cancelled = false
    var returnWithoutResult = false
    override fun transcribe(
        sourceId: Int?, sourceTitle: String?, fileName: String, audioFormat: String,
        byteSize: Long, audioSource: suspend (suspend (ByteArray) -> Unit) -> Unit,
    ): Flow<VocabularySourceTranscriptionUpdate> = flow {
        try {
            if (!returnWithoutResult) emit(result.await())
        } finally {
            cancelled = !result.isCompleted
        }
    }
}

private class CoordinatorTestBackground : IVocabularySourceTranscriptionBackgroundManager {
    val outcomes = mutableListOf<Pair<VocabularySourceTranscriptionOutcome, Boolean>>()
    override fun startTranscription(sourceId: Int, sourceTitle: String) = VocabularySourceTranscriptionHandle { outcome, sent ->
        outcomes += outcome to sent
    }
}

private fun VocabularySourceTranscriptionCoordinator.startTest() = start(1, "Source", "test.wav", "audio/wav", 1) {}

@OptIn(ExperimentalCoroutinesApi::class)
class VocabularySourceTranscriptionCoordinatorTest {
    @Test
    fun logoutCancelsActiveWorkAndRemovesRetainedResults() = runTest {
        val repository = CoordinatorTestRepository()
        val background = CoordinatorTestBackground()
        val coordinator = VocabularySourceTranscriptionCoordinator(
            TranscribeVocabularySourceAudioUseCase(repository), background, backgroundScope,
        )
        assertTrue(coordinator.startTest())
        runCurrent()
        coordinator.cancelAll()
        assertTrue(repository.cancelled)
        assertNull(coordinator.observe(1).first())
        assertEquals(listOf(VocabularySourceTranscriptionOutcome.Cancelled to false), background.outcomes)

        repository.result.complete(VocabularySourceTranscriptionUpdate.Success(
            VocabularySourceTranscriptionResult("New session", emptyList()),
        ))
        assertTrue(coordinator.startTest())
        runCurrent()
        assertFalse(coordinator.observe(1).first()!!.isActive)
        coordinator.cancelAll()
        assertNull(coordinator.observe(1).first())
    }

    @Test
    fun detachingScreenDoesNotCancelAndReturningScreenReceivesRetainedResult() = runTest {
        val repository = CoordinatorTestRepository()
        val background = CoordinatorTestBackground()
        val coordinator = VocabularySourceTranscriptionCoordinator(TranscribeVocabularySourceAudioUseCase(repository), background, backgroundScope)
        val collector = backgroundScope.launch { coordinator.observe(1).collect {} }
        assertTrue(coordinator.startTest())
        runCurrent()
        collector.cancel()
        runCurrent()
        assertFalse(repository.cancelled)
        val result = VocabularySourceTranscriptionUpdate.Success(VocabularySourceTranscriptionResult("Transcript", emptyList()), true)
        repository.result.complete(result)
        runCurrent()
        val operation = coordinator.observe(1).first()!!
        assertEquals(result, operation.update)
        assertFalse(operation.isActive)
        assertNull(operation.failure)
        assertEquals(listOf(VocabularySourceTranscriptionOutcome.Succeeded to true), background.outcomes)
    }

    @Test
    fun explicitCancelStopsWorkAndDuplicateStartDoesNotReplaceIt() = runTest {
        val repository = CoordinatorTestRepository()
        val background = CoordinatorTestBackground()
        val coordinator = VocabularySourceTranscriptionCoordinator(TranscribeVocabularySourceAudioUseCase(repository), background, backgroundScope)
        assertTrue(coordinator.startTest())
        runCurrent()
        val id = coordinator.observe(1).first()!!.id
        assertFalse(coordinator.startTest())
        assertEquals(id, coordinator.observe(1).first()!!.id)
        coordinator.cancel(1)
        runCurrent()
        assertTrue(repository.cancelled)
        assertFalse(coordinator.observe(1).first()!!.isActive)
        assertEquals(listOf(VocabularySourceTranscriptionOutcome.Cancelled to false), background.outcomes)
    }

    @Test
    fun missingTerminalResultIsFailureNotSuccess() = runTest {
        val repository = CoordinatorTestRepository().apply { returnWithoutResult = true }
        val background = CoordinatorTestBackground()
        val coordinator = VocabularySourceTranscriptionCoordinator(TranscribeVocabularySourceAudioUseCase(repository), background, backgroundScope)
        coordinator.startTest()
        runCurrent()
        assertIs<IllegalStateException>(coordinator.observe(1).first()!!.failure)
        assertEquals(listOf(VocabularySourceTranscriptionOutcome.Failed to false), background.outcomes)
    }

    @Test
    fun completedResultExpiresAtFifteenMinutesAndOldSaveCannotClearNewOperation() = runTest {
        var now = 0L
        val repository = CoordinatorTestRepository()
        val background = CoordinatorTestBackground()
        val coordinator = VocabularySourceTranscriptionCoordinator(TranscribeVocabularySourceAudioUseCase(repository), background, backgroundScope, { now })
        repository.result.complete(VocabularySourceTranscriptionUpdate.Success(VocabularySourceTranscriptionResult("Text", emptyList())))
        coordinator.startTest()
        runCurrent()
        val oldId = coordinator.observe(1).first()!!.id
        now = 899_999
        assertEquals(oldId, coordinator.observe(1).first()!!.id)
        now = 900_000
        assertNull(coordinator.observe(1).first())
        coordinator.startTest()
        runCurrent()
        coordinator.clearCompleted(1, oldId)
        assertTrue(coordinator.observe(1).first()!!.id != oldId)
    }
}
