package com.kuts.klaf.networking.openai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
internal data class OpenAiChatCompletionsRequest(
    val model: String,
    val messages: List<OpenAiChatMessage>,
    val temperature: Double,
    @SerialName("max_completion_tokens")
    val maxCompletionTokens: Int,
    @SerialName("reasoning_effort")
    val reasoningEffort: String,
    @SerialName("response_format")
    val responseFormat: OpenAiResponseFormat,
)

@Serializable
internal data class OpenAiChatMessage(
    val role: String,
    val content: String,
)

@Serializable
internal data class OpenAiResponseFormat(
    val type: String,
    @SerialName("json_schema")
    val jsonSchema: OpenAiJsonSchemaFormat,
)

@Serializable
internal data class OpenAiJsonSchemaFormat(
    val name: String,
    val strict: Boolean,
    val schema: JsonObject,
)

@Serializable
internal data class OpenAiChatCompletionsResponse(
    val choices: List<OpenAiChoice> = emptyList(),
)

@Serializable
internal data class OpenAiChoice(
    val message: OpenAiAssistantMessage? = null,
    @SerialName("finish_reason")
    val finishReason: String? = null,
)

@Serializable
internal data class OpenAiAssistantMessage(
    val role: String? = null,
    val content: String? = null,
    val refusal: String? = null,
)
