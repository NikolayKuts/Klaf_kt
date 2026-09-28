package com.kuts.klaf.networking.klafServer

import com.kuts.domain.entities.CefrLevel
import com.kuts.domain.entities.VocabularySourceAnalysis
import com.kuts.domain.entities.VocabularySourceAnalysisItem
import com.kuts.domain.entities.VocabularySourceAnalysisResult
import com.kuts.domain.entities.VocabularySourceItemConfidence
import com.kuts.domain.entities.VocabularySourceItemOccurrence
import com.kuts.domain.entities.VocabularySourceItemPartOfSpeech
import com.kuts.domain.repositories.IVocabularySourceAnalysisRepository
import com.kuts.klaf.server.contract.KlafServerErrorMessage
import com.kuts.klaf.server.contract.VocabularySourceAnalysisDto
import com.kuts.klaf.server.contract.VocabularySourceAnalysisItemDto
import com.kuts.klaf.server.contract.VocabularySourceAnalyzeRequest
import com.kuts.klaf.server.contract.VocabularySourceAnalyzedMessage
import com.kuts.klaf.server.contract.VocabularySourceItemConfidenceDto
import com.kuts.klaf.server.contract.VocabularySourceItemOccurrenceDto
import com.kuts.klaf.server.contract.VocabularySourceItemPartOfSpeechDto
import com.lib.lokdroid.core.logD
import com.lib.lokdroid.core.logE
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

class KlafServerVocabularySourceAnalysisRepository(
    private val klafServerSession: IKlafServerSession,
) : IVocabularySourceAnalysisRepository {

    override suspend fun analyze(
        cleanText: String,
        sourceId: Int?,
        sourceTitle: String?,
    ): VocabularySourceAnalysisResult {
        val text = cleanText.trim()
        require(text.isNotBlank()) { "Transcript text must not be blank." }

        val requestId = klafServerSession.nextRequestId(prefix = "vocabulary-source-analysis")
        val response = try {
            klafServerSession.request(
                message = VocabularySourceAnalyzeRequest(
                    requestId = requestId,
                    cleanText = text,
                    sourceId = sourceId,
                    sourceTitle = sourceTitle,
                ),
            )
        } catch (cancellation: CancellationException) {
            withContext(NonCancellable) {
                klafServerSession.cancelRequest(requestId = requestId)
            }
            throw cancellation
        } catch (throwable: Throwable) {
            logE("Klaf Server vocabulary source analysis request failed: requestId=$requestId, failure=$throwable")
            throw IllegalArgumentException("Klaf Server request failed.", throwable)
        }

        return when (response) {
            is VocabularySourceAnalyzedMessage -> {
                logD(
                    "Klaf Server vocabulary source analysis received: requestId=$requestId, " +
                        "language=${response.analysis.language}, items=${response.analysis.items.size}, " +
                        "serverNotificationSent=${response.serverNotificationSent}",
                )
                VocabularySourceAnalysisResult(
                    analysis = response.analysis.toDomainEntity(),
                    serverNotificationSent = response.serverNotificationSent,
                )
            }
            is KlafServerErrorMessage -> {
                logE(
                    "Klaf Server vocabulary source analysis error: requestId=$requestId, " +
                        "code=${response.code}, message=${response.message}",
                )
                throw IllegalArgumentException(response.message)
            }
            else -> error("Unexpected Klaf Server response: ${response::class.simpleName}")
        }
    }
}

private fun VocabularySourceAnalysisDto.toDomainEntity(): VocabularySourceAnalysis =
    VocabularySourceAnalysis(
        language = language,
        items = items.map { item -> item.toDomainEntity() },
    )

private fun VocabularySourceAnalysisItemDto.toDomainEntity(): VocabularySourceAnalysisItem =
    VocabularySourceAnalysisItem(
        foreignWord = foreignWord,
        transcription = transcription,
        nativeWord = nativeWord,
        originalText = originalText,
        partOfSpeech = partOfSpeech.toDomainEntity(),
        cefrLevel = cefrLevel?.toEnumOrDefault<CefrLevel>(),
        confidence = confidence.toDomainEntity(),
        sourceExample = sourceExample,
        explanation = explanation,
        occurrences = occurrences.map { occurrence -> occurrence.toDomainEntity() },
    )

private fun VocabularySourceItemOccurrenceDto.toDomainEntity(): VocabularySourceItemOccurrence =
    VocabularySourceItemOccurrence(
        timestamp = timestamp,
        startOffset = startOffset,
        endOffset = endOffset,
        sentence = sentence,
    )

private fun VocabularySourceItemPartOfSpeechDto.toDomainEntity(): VocabularySourceItemPartOfSpeech =
    VocabularySourceItemPartOfSpeech.valueOf(name)

private fun VocabularySourceItemConfidenceDto.toDomainEntity(): VocabularySourceItemConfidence =
    VocabularySourceItemConfidence.valueOf(name)

private inline fun <reified T : Enum<T>> String.toEnumOrDefault(): T? =
    enumValues<T>().firstOrNull { value -> value.name == this }
