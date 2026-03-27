package com.kuts.klaf.networking.openai

import com.kuts.domain.entities.WordMeaningInsights
import com.kuts.domain.repositories.IWordMeaningInsightsRepository
import com.kuts.klaf.SecretConstants
import com.kuts.klaf.common.WordMeaningInsightsPayload
import com.kuts.klaf.common.toDomainEntity
import com.kuts.klaf.networking.wordInsights.WordMeaningInsightsPrompt
import com.kuts.klaf.networking.wordInsights.WordMeaningInsightsPromptFactory
import com.kuts.klaf.networking.wordInsights.isValidForContract
import com.kuts.klaf.networking.wordInsights.throwIfInvalidForRequestedWord
import com.kuts.klaf.networking.wordInsights.unwrapMarkdownCodeFence
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

class OpenAiWordMeaningInsightsRepository(
    private val client: HttpClient,
) : IWordMeaningInsightsRepository {

    companion object {

        private const val EMPTY_SECRET_PLACEHOLDER = "empty"
        private const val STRUCTURED_OUTPUT_NAME = "word_meaning_insights"
        private const val RESPONSE_FORMAT_TYPE = "json_schema"
        private const val REASONING_EFFORT = "none"
        private const val TEMPERATURE = 0.2
        private const val MAX_OUTPUT_TOKENS = 1024
        private const val TIMEOUT_REQUEST_ATTEMPTS = 2
        private const val TIMEOUT_RETRY_DELAY_MS = 1_500L
        private const val DNS_REQUEST_ATTEMPTS = 2
        private const val DNS_RETRY_DELAY_MS = 1_500L
        private const val AUTHORIZATION_SCHEME = "Bearer"
        private val UNSUPPORTED_STRUCTURED_OUTPUT_KEYWORDS = setOf(
            "uniqueItems",
            "minLength",
            "maxLength",
            "minimum",
            "maximum",
        )
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
        prettyPrint = true
    }

    override suspend fun fetchWordMeaningInsights(word: String): WordMeaningInsights {
        val apiKey = SecretConstants.OpenAiApi.apiKeyOrNull()
        require(value = apiKey != null && apiKey != EMPTY_SECRET_PLACEHOLDER) {
            "OpenAI API key is not configured."
        }

        val model = SecretConstants.OpenAiApi.modelOrNull()
        require(value = !model.isNullOrBlank()) {
            "OpenAI model is not configured."
        }

        val requestedWord = word.trim()
        val prompt = WordMeaningInsightsPromptFactory.build(word = requestedWord)

        val response = executeChatCompletionsRequest(
            apiKey = apiKey,
            model = model,
            prompt = prompt,
        )

        val responseBodyText = response.bodyAsText()
        require(value = response.status.isSuccess()) {
            "OpenAI request failed. HTTP ${response.status.value}. Body: $responseBodyText"
        }

        val responseBody = json.decodeFromString(
            deserializer = OpenAiChatCompletionsResponse.serializer(),
            string = responseBodyText,
        )
        val assistantMessage = responseBody.choices
            .firstOrNull()
            ?.message
            ?: throw IllegalArgumentException("OpenAI returned no choices for word insights.")

        assistantMessage.refusal?.takeIf { refusal -> refusal.isNotBlank() }?.let { refusal ->
            throw IllegalArgumentException("OpenAI refused to generate insights. $refusal")
        }

        val rawJson = assistantMessage.content.orEmpty().trim()
        require(value = rawJson.isNotEmpty()) {
            "OpenAI returned empty insights payload. Body: $responseBodyText"
        }

        val payload = json.decodeFromString(
            deserializer = WordMeaningInsightsPayload.serializer(),
            string = rawJson.unwrapMarkdownCodeFence(),
        )
        payload.throwIfInvalidForRequestedWord(requestedWord = requestedWord)

        val parsedInsights = payload.toDomainEntity()
        require(value = parsedInsights.isValidForContract(expectedWord = requestedWord)) {
            "OpenAI returned payload that does not match the expected contract."
        }

        return parsedInsights
    }

    private fun buildUrl(): String {
        return "${SecretConstants.OpenAiApi.BASE_URL.trimEnd('/')}/chat/completions"
    }

    private suspend fun executeChatCompletionsRequest(
        apiKey: String,
        model: String,
        prompt: WordMeaningInsightsPrompt,
    ): HttpResponse {
        var timeoutRequestAttempt = 0
        var dnsRequestAttempt = 0

        while (true) {
            try {
                return client.post(urlString = buildUrl()) {
                    header(HttpHeaders.Authorization, "$AUTHORIZATION_SCHEME $apiKey")
                    contentType(ContentType.Application.Json)
                    setBody(
                        OpenAiChatCompletionsRequest(
                            model = model,
                            messages = listOf(
                                OpenAiChatMessage(
                                    role = "system",
                                    content = prompt.systemInstruction,
                                ),
                                OpenAiChatMessage(
                                    role = "user",
                                    content = prompt.userPrompt,
                                ),
                            ),
                            temperature = TEMPERATURE,
                            maxCompletionTokens = MAX_OUTPUT_TOKENS,
                            reasoningEffort = REASONING_EFFORT,
                            responseFormat = OpenAiResponseFormat(
                                type = RESPONSE_FORMAT_TYPE,
                                jsonSchema = OpenAiJsonSchemaFormat(
                                    name = STRUCTURED_OUTPUT_NAME,
                                    strict = true,
                                    schema = prompt.responseJsonSchema
                                        .toJsonObject()
                                        .removeUnsupportedStructuredOutputKeywords()
                                        .jsonObject,
                                ),
                            ),
                        ),
                    )
                }
            } catch (_: HttpRequestTimeoutException) {
                timeoutRequestAttempt++
                if (timeoutRequestAttempt >= TIMEOUT_REQUEST_ATTEMPTS) {
                    throw IllegalArgumentException(
                        "OpenAI request timed out after $TIMEOUT_REQUEST_ATTEMPTS attempts."
                    )
                }
                delay(timeMillis = TIMEOUT_RETRY_DELAY_MS)
            } catch (networkError: IOException) {
                if (networkError.isDnsResolutionFailure()) {
                    dnsRequestAttempt++
                    if (dnsRequestAttempt >= DNS_REQUEST_ATTEMPTS) {
                        throw IllegalArgumentException(
                            "Cannot reach OpenAI host. Check Internet connection, VPN, or DNS settings."
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

    private fun JsonElement.removeUnsupportedStructuredOutputKeywords(): JsonElement {
        return when (this) {
            is JsonObject -> JsonObject(
                content = entries
                    .asSequence()
                    .filterNot { (key, _) -> key in UNSUPPORTED_STRUCTURED_OUTPUT_KEYWORDS }
                    .associate { (key, value) ->
                        key to value.removeUnsupportedStructuredOutputKeywords()
                    },
            )

            is JsonArray -> JsonArray(
                content = map { element -> element.removeUnsupportedStructuredOutputKeywords() },
            )

            else -> this
        }
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
