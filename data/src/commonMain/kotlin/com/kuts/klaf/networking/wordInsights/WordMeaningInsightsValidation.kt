package com.kuts.klaf.networking.wordInsights

import com.kuts.domain.entities.WordMeaningInsights
import com.kuts.klaf.common.WordMeaningInsightsPayload

internal fun String.unwrapMarkdownCodeFence(): String = trim()
    .removePrefix("```json")
    .removePrefix("```")
    .removeSuffix("```")
    .trim()

internal fun WordMeaningInsights.isValidForContract(expectedWord: String): Boolean {
    if (word.isBlank()) return false
    if (word.toWordKey() != expectedWord.toWordKey()) return false
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

internal fun WordMeaningInsightsPayload.throwIfInvalidForRequestedWord(requestedWord: String) {
    if (word.toWordKey() != requestedWord.toWordKey()) {
        throw IllegalArgumentException(
            "LLM returned mismatched word. expected=$requestedWord, actual=$word"
        )
    }

    if (!isWordValid) {
        throw IllegalArgumentException(
            invalidReason.ifBlank { "The provided token is not a valid English word." }
        )
    }
}

private fun String.toWordKey(): String = trim().lowercase()
