package com.kuts.domain.repositories

import com.kuts.domain.entities.SpeechToTextSegment
import kotlinx.coroutines.flow.Flow

sealed interface VocabularySourceTranscriptionProgress {
    data class Uploading(val uploadedBytes: Long, val totalBytes: Long) : VocabularySourceTranscriptionProgress
    data class Transcribing(val completedChunks: Int, val totalChunks: Int) : VocabularySourceTranscriptionProgress
}

data class VocabularySourceTranscriptionResult(
    val transcript: String,
    val segments: List<SpeechToTextSegment>,
)

sealed interface VocabularySourceTranscriptionUpdate {
    data class Progress(val progress: VocabularySourceTranscriptionProgress) : VocabularySourceTranscriptionUpdate
    data class Success(
        val result: VocabularySourceTranscriptionResult,
        val serverNotificationSent: Boolean = false,
    ) : VocabularySourceTranscriptionUpdate
}

interface IVocabularySourceTranscriptionRepository {

    fun transcribe(
        sourceId: Int?,
        sourceTitle: String? = null,
        fileName: String,
        audioFormat: String,
        byteSize: Long,
        audioSource: suspend (writeChunk: suspend (ByteArray) -> Unit) -> Unit,
    ): Flow<VocabularySourceTranscriptionUpdate>
}
