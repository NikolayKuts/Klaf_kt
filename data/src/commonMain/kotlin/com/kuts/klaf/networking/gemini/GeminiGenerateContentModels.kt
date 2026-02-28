package com.kuts.klaf.networking.gemini

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
internal data class GeminiGenerateContentRequest(
    @SerialName("systemInstruction")
    val systemInstruction: GeminiContent,
    val contents: List<GeminiContent>,
    @SerialName("generationConfig")
    val generationConfig: GeminiGenerationConfig,
)

@Serializable
internal data class GeminiGenerationConfig(
    @SerialName("responseMimeType")
    val responseMimeType: String,
    @SerialName("responseSchema")
    val responseSchema: JsonObject,
    val temperature: Double,
    @SerialName("maxOutputTokens")
    val maxOutputTokens: Int,
)

@Serializable
internal data class GeminiGenerateContentResponse(
    val candidates: List<GeminiCandidate> = emptyList(),
    @SerialName("promptFeedback")
    val promptFeedback: GeminiPromptFeedback? = null,
)

@Serializable
internal data class GeminiCandidate(
    val content: GeminiContent? = null,
    @SerialName("finishReason")
    val finishReason: String? = null,
)

@Serializable
internal data class GeminiPromptFeedback(
    @SerialName("blockReason")
    val blockReason: String? = null,
)

@Serializable
internal data class GeminiContent(
    val parts: List<GeminiPart> = emptyList(),
)

@Serializable
internal data class GeminiPart(
    val text: String? = null,
)

