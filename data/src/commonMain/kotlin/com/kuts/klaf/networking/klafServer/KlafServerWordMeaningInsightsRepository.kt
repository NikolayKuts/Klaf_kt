package com.kuts.klaf.networking.klafServer

import com.kuts.domain.entities.CefrLevel
import com.kuts.domain.entities.WordMeaningInsights
import com.kuts.domain.entities.WordMeaningItem
import com.kuts.domain.managers.CardLaunchContext
import com.kuts.domain.repositories.IWordMeaningInsightsRepository
import com.kuts.klaf.server.contract.CardLaunchContextDto
import com.kuts.klaf.server.contract.KlafServerErrorMessage
import com.kuts.klaf.server.contract.WordInsightsGenerateRequest
import com.kuts.klaf.server.contract.WordInsightsGeneratedMessage
import com.kuts.klaf.server.contract.WordMeaningInsightsDto
import com.kuts.klaf.server.contract.WordMeaningItemDto
import com.lib.lokdroid.core.logD
import com.lib.lokdroid.core.logE
import kotlinx.coroutines.CancellationException

class KlafServerWordMeaningInsightsRepository(
    private val klafServerSession: IKlafServerSession,
) : IWordMeaningInsightsRepository {

    override suspend fun fetchWordMeaningInsights(
        word: String,
        launchContext: CardLaunchContext?,
    ): WordMeaningInsights {
        val requestedWord = word.trim()
        require(requestedWord.isNotBlank()) { "Word must not be blank." }

        val requestId = klafServerSession.nextRequestId(prefix = "word-insights")
        val response = try {
            klafServerSession.request(
                message = WordInsightsGenerateRequest(
                    requestId = requestId,
                    word = requestedWord,
                    launchContext = launchContext?.toContractDto(),
                ),
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            logE("Klaf Server Word Insights request failed: requestId=$requestId, failure=$throwable")
            throw IllegalArgumentException("Klaf Server request failed.", throwable)
        }

        return when (response) {
            is WordInsightsGeneratedMessage -> {
                logD(
                    "Klaf Server Word Insights received: requestId=$requestId, " +
                        "word=${response.insights.word}, meanings=${response.insights.meanings.size}",
                )
                response.insights.toDomainEntity()
            }
            is KlafServerErrorMessage -> {
                logE(
                    "Klaf Server Word Insights error: requestId=$requestId, " +
                        "code=${response.code}, message=${response.message}",
                )
                throw IllegalArgumentException(response.message)
            }
            else -> error("Unexpected Klaf Server response: ${response::class.simpleName}")
        }
    }
}

private fun WordMeaningInsightsDto.toDomainEntity(): WordMeaningInsights =
    WordMeaningInsights(
        word = word,
        language = language,
        meanings = meanings.map { item -> item.toDomainEntity() },
    )

private fun WordMeaningItemDto.toDomainEntity(): WordMeaningItem =
    WordMeaningItem(
        frequencyRank = frequencyRank,
        translation = translation,
        proficiencyLevel = CefrLevel.valueOf(cefr),
        context = context,
        examples = examples,
    )

private fun CardLaunchContext.toContractDto(): CardLaunchContextDto =
    CardLaunchContextDto(
        deckId = deckId,
        cardId = cardId,
    )
