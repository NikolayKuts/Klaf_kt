package com.kuts.klaf.networking.gemini

import com.kuts.domain.entities.WordMeaningInsights
import com.kuts.domain.repositories.IWordMeaningInsightsRepository
import com.kuts.klaf.SecretConstants
import com.kuts.klaf.common.WordMeaningInsightsPayload
import com.kuts.klaf.common.toDomainEntity
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

class GeminiWordMeaningInsightsProvider(
    private val client: HttpClient,
) : IWordMeaningInsightsRepository {

    companion object {

        private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com"
        private const val GEMINI_API_VERSION = "v1beta"
        private const val API_KEY_QUERY_PARAMETER = "key"
        private const val EMPTY_SECRET_PLACEHOLDER = "empty"
        private const val RESPONSE_MIME_TYPE_JSON = "application/json"
        private const val TEMPERATURE = 0.2
        private const val MAX_OUTPUT_TOKENS = 1024
        private const val TIMEOUT_REQUEST_ATTEMPTS = 2
        private const val TIMEOUT_RETRY_DELAY_MS = 1_500L
        private const val DNS_REQUEST_ATTEMPTS = 2
        private const val DNS_RETRY_DELAY_MS = 1_500L
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
        prettyPrint = true
    }

    override suspend fun fetchWordMeaningInsights(word: String): WordMeaningInsights {
        val apiKey = SecretConstants.GeminiApi.GEMINI_API_KEY
        require(value = apiKey.isNotBlank() && apiKey != EMPTY_SECRET_PLACEHOLDER) {
            "Gemini API key is not configured."
        }

        val prompt = WordMeaningInsightsPromptFactory.build(word = word)
        val model = SecretConstants.GeminiApi.GEMINI_MODEL

        val response = executeGenerateContentRequest(
            apiKey = apiKey,
            model = model,
            prompt = prompt,
        )

        val responseBodyText = response.bodyAsText()
        require(value = response.status.isSuccess()) {
            "Gemini request failed. HTTP ${response.status.value}. Body: $responseBodyText"
        }

        val responseBody = json.decodeFromString(
            deserializer = GeminiGenerateContentResponse.serializer(),
            string = responseBodyText,
        )
        val rawJson = responseBody
            .candidates
            .flatMap { candidate -> candidate.content?.parts.orEmpty() }
            .mapNotNull { part -> part.text?.trim() }
            .joinToString(separator = "")
            .orEmpty()

        require(value = rawJson.isNotEmpty()) {
            val finishReasons = responseBody.candidates.mapNotNull { it.finishReason }.distinct()
            val blockReason = responseBody.promptFeedback?.blockReason
            "Gemini returned empty insights payload. finishReasons=$finishReasons, blockReason=$blockReason, body=$responseBodyText"
        }

        val parsedInsights = json.decodeFromString(
            deserializer = WordMeaningInsightsPayload.serializer(),
            string = rawJson.unwrapMarkdownCodeFence(),
        ).toDomainEntity()
        require(value = parsedInsights.isValid()) {
            "Gemini returned payload that does not match the expected contract."
        }

        return parsedInsights
    }

    private fun buildUrl(model: String): String {
        return "$GEMINI_BASE_URL/$GEMINI_API_VERSION/models/$model:generateContent"
    }

    private suspend fun executeGenerateContentRequest(
        apiKey: String,
        model: String,
        prompt: WordMeaningInsightsPrompt,
    ): HttpResponse {
        var timeoutRequestAttempt = 0
        var dnsRequestAttempt = 0

        while (true) {
            try {
                return client.post(urlString = buildUrl(model = model)) {
                    url {
                        parameters.append(name = API_KEY_QUERY_PARAMETER, value = apiKey)
                    }
                    contentType(ContentType.Application.Json)
                    setBody(
                        GeminiGenerateContentRequest(
                            systemInstruction = GeminiContent(
                                parts = listOf(GeminiPart(text = prompt.systemInstruction)),
                            ),
                            contents = listOf(
                                GeminiContent(parts = listOf(GeminiPart(text = prompt.userPrompt))),
                            ),
                            generationConfig = GeminiGenerationConfig(
                                responseMimeType = RESPONSE_MIME_TYPE_JSON,
                                responseSchema = WordMeaningInsightsContract.responseApiSchema.toJsonObject(),
                                temperature = TEMPERATURE,
                                maxOutputTokens = MAX_OUTPUT_TOKENS,
                            ),
                        ),
                    )
                }
            } catch (_: HttpRequestTimeoutException) {
                timeoutRequestAttempt++
                if (timeoutRequestAttempt >= TIMEOUT_REQUEST_ATTEMPTS) {
                    throw IllegalArgumentException(
                        "Gemini request timed out after $TIMEOUT_REQUEST_ATTEMPTS attempts."
                    )
                }
                delay(timeMillis = TIMEOUT_RETRY_DELAY_MS)
            } catch (networkError: IOException) {
                if (networkError.isDnsResolutionFailure()) {
                    dnsRequestAttempt++
                    if (dnsRequestAttempt >= DNS_REQUEST_ATTEMPTS) {
                        throw IllegalArgumentException(
                            "Cannot reach Gemini host. Check Internet connection, VPN, or DNS settings."
                        )
                    }
                    delay(timeMillis = DNS_RETRY_DELAY_MS)
                } else {
                    throw networkError
                }
            }
        }
    }

    private fun String.toJsonObject(): JsonObject {
        return json.parseToJsonElement(this).jsonObject
    }
}

private fun String.unwrapMarkdownCodeFence(): String = trim()
    .removePrefix("```json")
    .removePrefix("```")
    .removeSuffix("```")
    .trim()

private fun WordMeaningInsights.isValid(): Boolean {
    if (word.isBlank()) return false
    if (language != WordMeaningInsightsContract.LANGUAGE) return false
    if (meanings.size !in WordMeaningInsightsContract.MIN_SENSES_COUNT..WordMeaningInsightsContract.MAX_SENSES_COUNT) {
        return false
    }

    val ranks = mutableSetOf<Int>()
    return meanings.all { meaning ->
        val hasValidRank = ranks.add(meaning.frequencyRank)
            && meaning.frequencyRank in 1..WordMeaningInsightsContract.MAX_SENSES_COUNT
        val hasValidTranslation = meaning.translation.isNotBlank()
        val hasValidContext = meaning.context.isNotBlank()
        val hasValidExamples = meaning.examples.size == WordMeaningInsightsContract.EXAMPLES_PER_SENSE
            && meaning.examples.distinct().size == WordMeaningInsightsContract.EXAMPLES_PER_SENSE
            && meaning.examples.all { example -> example.isNotBlank() }
        hasValidRank && hasValidTranslation && hasValidContext && hasValidExamples
    }
}

private fun Throwable.isDnsResolutionFailure(): Boolean {
    var current: Throwable? = this
    while (current != null) {
        val message = current.message.orEmpty().lowercase()
        val hasDnsFailureMessage = message.contains("unable to resolve host")
            || message.contains("unknown host")
            || message.contains("name or service not known")
            || message.contains("nodename nor servname")
            || message.contains("eai_nodata")
        if (hasDnsFailureMessage) return true
        current = current.cause
    }
    return false
}
