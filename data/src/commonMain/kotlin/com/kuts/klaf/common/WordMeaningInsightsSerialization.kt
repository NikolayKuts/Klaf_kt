package com.kuts.klaf.common

import com.kuts.domain.entities.CefrLevel
import com.kuts.domain.entities.WordMeaningInsights
import com.kuts.domain.entities.WordMeaningItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class WordMeaningInsightsPayload(
    val word: String = "",
    @SerialName("word_valid")
    val isWordValid: Boolean = true,
    @SerialName("invalid_reason")
    val invalidReason: String = "",
    val language: String = "",
    @SerialName("senses")
    val meanings: List<WordMeaningItemPayload> = emptyList(),
)

@Serializable
internal data class WordMeaningItemPayload(
    @SerialName("rank")
    val frequencyRank: Int = 0,
    @SerialName("translation_ru")
    val translation: String = "",
    @SerialName("cefr")
    val proficiencyLevel: CefrLevel = CefrLevel.A1,
    val context: String = "",
    @SerialName("examples_en")
    val examples: List<String> = emptyList(),
)

internal fun WordMeaningInsightsPayload.toDomainEntity(): WordMeaningInsights {
    return WordMeaningInsights(
        word = word,
        language = language,
        meanings = meanings.map { item -> item.toDomainEntity() },
    )
}

internal fun WordMeaningInsights.toPayload(): WordMeaningInsightsPayload {
    return WordMeaningInsightsPayload(
        word = word,
        isWordValid = true,
        invalidReason = "",
        language = language,
        meanings = meanings.map { item -> item.toPayload() },
    )
}

private fun WordMeaningItemPayload.toDomainEntity(): WordMeaningItem {
    return WordMeaningItem(
        frequencyRank = frequencyRank,
        translation = translation,
        proficiencyLevel = proficiencyLevel,
        context = context,
        examples = examples,
    )
}

private fun WordMeaningItem.toPayload(): WordMeaningItemPayload {
    return WordMeaningItemPayload(
        frequencyRank = frequencyRank,
        translation = translation,
        proficiencyLevel = proficiencyLevel,
        context = context,
        examples = examples,
    )
}
