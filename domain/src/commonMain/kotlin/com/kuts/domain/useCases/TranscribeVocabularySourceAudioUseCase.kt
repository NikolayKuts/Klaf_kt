package com.kuts.domain.useCases

import com.kuts.domain.repositories.IVocabularySourceTranscriptionRepository
import com.kuts.domain.repositories.VocabularySourceTranscriptionUpdate
import kotlinx.coroutines.flow.Flow

class TranscribeVocabularySourceAudioUseCase(
    private val repository: IVocabularySourceTranscriptionRepository,
) {

    operator fun invoke(
        sourceId: Int?,
        sourceTitle: String? = null,
        fileName: String,
        audioFormat: String,
        byteSize: Long,
        audioSource: suspend (writeChunk: suspend (ByteArray) -> Unit) -> Unit,
    ): Flow<VocabularySourceTranscriptionUpdate> = repository.transcribe(
        sourceId = sourceId,
        sourceTitle = sourceTitle,
        fileName = fileName,
        audioFormat = audioFormat,
        byteSize = byteSize,
        audioSource = audioSource,
    )
}
