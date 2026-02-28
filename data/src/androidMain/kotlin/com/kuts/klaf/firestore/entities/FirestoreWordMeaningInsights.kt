package com.kuts.klaf.firestore.entities

data class FirestoreWordMeaningInsights(
    val word: String = "",
    val language: String = "",
    val meanings: List<FirestoreWordMeaningItem> = emptyList(),
)

data class FirestoreWordMeaningItem(
    val frequencyRank: Int = 0,
    val translation: String = "",
    val proficiencyLevel: String = "",
    val context: String = "",
    val examples: List<String> = emptyList(),
)
