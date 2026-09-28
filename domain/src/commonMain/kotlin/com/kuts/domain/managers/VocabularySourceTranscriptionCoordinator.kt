package com.kuts.domain.managers

import com.kuts.domain.common.getCurrentDateAsLong
import com.kuts.domain.repositories.VocabularySourceTranscriptionProgress
import com.kuts.domain.repositories.VocabularySourceTranscriptionUpdate
import com.kuts.domain.useCases.TranscribeVocabularySourceAudioUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

private const val COMPLETED_TRANSCRIPTION_RETENTION_MILLIS = 15 * 60 * 1_000L

data class VocabularySourceTranscriptionOperation(
    val id: Long,
    val update: VocabularySourceTranscriptionUpdate?,
    val isActive: Boolean,
    val failure: Throwable? = null,
    val completedAt: Long? = null,
)

private data class TranscriptionEntry(
    val operation: VocabularySourceTranscriptionOperation,
    val job: Job,
)

/** Process-owned work. Detaching a screen is not an explicit Cancel action. */
class VocabularySourceTranscriptionCoordinator(
    private val transcribe: TranscribeVocabularySourceAudioUseCase,
    private val backgroundManager: IVocabularySourceTranscriptionBackgroundManager,
    private val scope: CoroutineScope,
    private val nowMillis: () -> Long = ::getCurrentDateAsLong,
    private val onBackgroundFailure: (Throwable) -> Unit = {},
) {
    private val entries = MutableStateFlow<Map<Int, TranscriptionEntry>>(emptyMap())

    fun observe(sourceId: Int): Flow<VocabularySourceTranscriptionOperation?> {
        pruneCompleted()
        return entries.map { it[sourceId]?.operation }.distinctUntilChanged()
    }

    fun start(
        sourceId: Int,
        sourceTitle: String,
        fileName: String,
        audioFormat: String,
        byteSize: Long,
        audioSource: suspend (suspend (ByteArray) -> Unit) -> Unit,
    ): Boolean {
        pruneCompleted()
        val id = Random.nextLong()
        var failure: Throwable? = null
        var serverNotificationSent = false
        val job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                var receivedResult = false
                transcribe(sourceId, sourceTitle, fileName, audioFormat, byteSize, audioSource).collect { update ->
                    if (update is VocabularySourceTranscriptionUpdate.Success) {
                        receivedResult = true
                        serverNotificationSent = update.serverNotificationSent
                    }
                    updateOperation(sourceId, id) { it.copy(update = update) }
                }
                check(receivedResult) { "Transcription ended without a result." }
            } catch (throwable: Throwable) {
                failure = throwable
                if (throwable is CancellationException) throw throwable
            }
        }
        val operation = VocabularySourceTranscriptionOperation(
            id = id,
            update = VocabularySourceTranscriptionUpdate.Progress(
                VocabularySourceTranscriptionProgress.Uploading(0, byteSize),
            ),
            isActive = true,
        )
        while (true) {
            val current = entries.value
            if (current[sourceId]?.operation?.isActive == true) {
                job.cancel()
                return false
            }
            if (entries.compareAndSet(current, current + (sourceId to TranscriptionEntry(operation, job)))) break
        }
        val handle = try {
            backgroundManager.startTranscription(sourceId, sourceTitle)
        } catch (throwable: Throwable) {
            job.cancel()
            updateOperation(sourceId, id) { it.copy(isActive = false, failure = throwable, completedAt = nowMillis()) }
            return false
        }
        job.invokeOnCompletion { cancellation ->
            val actualFailure = failure ?: cancellation
            updateOperation(sourceId, id) { it.copy(isActive = false, failure = actualFailure, completedAt = nowMillis()) }
            val outcome = when (actualFailure) {
                null -> VocabularySourceTranscriptionOutcome.Succeeded
                is CancellationException -> VocabularySourceTranscriptionOutcome.Cancelled
                else -> VocabularySourceTranscriptionOutcome.Failed
            }
            runCatching { handle.finish(outcome, serverNotificationSent) }.onFailure(onBackgroundFailure)
        }
        job.start()
        return true
    }

    fun cancel(sourceId: Int) {
        entries.value[sourceId]?.job?.cancel()
    }

    fun clearCompleted(sourceId: Int, operationId: Long) {
        entries.update { current ->
            val operation = current[sourceId]?.operation
            if (operation?.id == operationId && !operation.isActive) current - sourceId else current
        }
    }

    private fun updateOperation(
        sourceId: Int,
        id: Long,
        transform: (VocabularySourceTranscriptionOperation) -> VocabularySourceTranscriptionOperation,
    ) {
        entries.update { current ->
            val entry = current[sourceId]
            if (entry?.operation?.id == id) {
                current + (sourceId to entry.copy(operation = transform(entry.operation)))
            } else {
                current
            }
        }
    }

    private fun pruneCompleted() {
        val now = nowMillis()
        entries.update { current ->
            current.filterValues { entry ->
                entry.operation.completedAt?.let { now - it < COMPLETED_TRANSCRIPTION_RETENTION_MILLIS } ?: true
            }
        }
    }
}
