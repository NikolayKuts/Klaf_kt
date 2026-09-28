package com.kuts.klaf.networking.klafServer

import com.kuts.domain.entities.SpeechToTextSegment
import com.kuts.domain.repositories.IVocabularySourceTranscriptionRepository
import com.kuts.domain.repositories.VocabularySourceTranscriptionProgress
import com.kuts.domain.repositories.VocabularySourceTranscriptionResult
import com.kuts.domain.repositories.VocabularySourceTranscriptionUpdate
import com.kuts.klaf.server.contract.SpeechToTextSegmentDto
import com.lib.lokdroid.core.logD
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

private fun SpeechToTextSegmentDto.toDomain(): SpeechToTextSegment =
    SpeechToTextSegment(
        startMillis = startMillis,
        endMillis = endMillis,
        text = text,
    )

class KlafServerVocabularySourceTranscriptionRepository(
    private val klafServerSession: IKlafServerSession,
) : IVocabularySourceTranscriptionRepository {

    override fun transcribe(
        sourceId: Int?,
        sourceTitle: String?,
        fileName: String,
        audioFormat: String,
        byteSize: Long,
        audioSource: suspend (writeChunk: suspend (ByteArray) -> Unit) -> Unit,
    ): Flow<VocabularySourceTranscriptionUpdate> = flow {
        val requestIdPrefix = if (sourceId != null) "tx-source-$sourceId" else "tx-source"
        val requestId = klafServerSession.nextRequestId(prefix = requestIdPrefix)
        logD(
            "Starting vocabulary source transcription: requestId=$requestId, " +
                "sourceId=$sourceId, sourceTitle=$sourceTitle, fileName=$fileName, size=$byteSize",
        )
        klafServerSession.transcribeAudio(
            requestId = requestId,
            sourceId = sourceId,
            sourceTitle = sourceTitle,
            fileName = fileName,
            audioFormat = audioFormat,
            declaredByteSize = byteSize,
            audioStreamProvider = audioSource,
        ).collect { sessionEvent ->
            when (sessionEvent) {
                is VocabularySourceTranscriptionSessionEvent.UploadProgress -> {
                    emit(
                        VocabularySourceTranscriptionUpdate.Progress(
                            progress = VocabularySourceTranscriptionProgress.Uploading(
                                uploadedBytes = sessionEvent.uploadedBytes,
                                totalBytes = sessionEvent.totalBytes,
                            ),
                        ),
                    )
                }
                is VocabularySourceTranscriptionSessionEvent.RecognitionProgress -> {
                    emit(
                        VocabularySourceTranscriptionUpdate.Progress(
                            progress = VocabularySourceTranscriptionProgress.Transcribing(
                                completedChunks = sessionEvent.completedChunks,
                                totalChunks = sessionEvent.totalChunks,
                            ),
                        ),
                    )
                }
                is VocabularySourceTranscriptionSessionEvent.Completed -> {
                    logD(
                        "Vocabulary source transcription completed: requestId=$requestId, " +
                            "transcriptLength=${sessionEvent.transcript.length}, segments=${sessionEvent.segments.size}, " +
                            "serverNotificationSent=${sessionEvent.serverNotificationSent}",
                    )
                    emit(
                        VocabularySourceTranscriptionUpdate.Success(
                            result = VocabularySourceTranscriptionResult(
                                transcript = sessionEvent.transcript,
                                segments = sessionEvent.segments.map { it.toDomain() },
                            ),
                            serverNotificationSent = sessionEvent.serverNotificationSent,
                        ),
                    )
                }
            }
        }
    }
}
