package com.kuts.klaf.networking.agentDriver

import com.kuts.klaf.common.WordMeaningInsightsPayload
import com.kuts.klaf.common.toDomainEntity
import com.kuts.domain.entities.WordMeaningInsights
import com.kuts.domain.repositories.IWordMeaningInsightsRepository
import com.kuts.klaf.networking.wordInsights.WordMeaningInsightsPromptFactory
import com.kuts.klaf.networking.wordInsights.isValidForContract
import com.kuts.klaf.networking.wordInsights.throwIfInvalidForRequestedWord
import com.lib.lokdroid.core.logD
import com.lib.lokdroid.core.logE
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import org.agentdriver.project.protocol.TextGenerationRequest

class AgentDriverWordMeaningInsightsRepository(
    private val agentDriverSession: AgentDriverSession,
) : IWordMeaningInsightsRepository {

    @Suppress("OPT_IN_USAGE")
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    override suspend fun fetchWordMeaningInsights(word: String): WordMeaningInsights {
        val requestedWord = word.trim()
        require(value = requestedWord.isNotBlank()) { "Word must not be blank." }

        val prompt = WordMeaningInsightsPromptFactory.build(word = requestedWord)
        val rawResponse = try {
            agentDriverSession.generateText(
                request = TextGenerationRequest(
                    prompt = prompt.text,
                    responseSchema = prompt.responseSchema,
                ),
            ).also { response ->
                logD("Word insights for \"$requestedWord\": ${response.length} characters")
            }
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (throwable: Throwable) {
            logE("Word insights request failed: ${throwable::class.simpleName} -- $throwable")
            throw IllegalArgumentException(
                "Agent Driver request failed. ${throwable.toShortAgentDriverMessage()}",
                throwable,
            )
        }

        val payload = json.decodeFromString(
            deserializer = WordMeaningInsightsPayload.serializer(),
            string = rawResponse,
        )
        payload.throwIfInvalidForRequestedWord(requestedWord = requestedWord)

        val parsedInsights = payload.toDomainEntity()
        require(value = parsedInsights.isValidForContract(expectedWord = requestedWord)) {
            "The assistant returned a payload that does not match the expected contract."
        }

        return parsedInsights
    }
}
