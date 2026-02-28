package com.kuts.domain.entities

import kotlinx.serialization.Serializable

@Serializable
data class WordMeaningInsights(
    val word: String = "",
    val language: String = "",
    val meanings: List<WordMeaningItem> = emptyList(),
) {

    companion object {

        val EMPTY = WordMeaningInsights()
    }

    fun hasData(): Boolean {
        return word.isNotBlank() && meanings.isNotEmpty()
    }
}

@Serializable
data class WordMeaningItem(
    val frequencyRank: Int = 0,
    val translation: String = "",
    val proficiencyLevel: CefrLevel = CefrLevel.A1,
    val context: String = "",
    val examples: List<String> = emptyList(),
)

@Serializable
enum class CefrLevel {
    A1,
    A2,
    B1,
    B2,
    C1,
    C2,
}
