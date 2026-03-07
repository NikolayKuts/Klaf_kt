package com.kuts.klaf.cardManagement.common

data class CambridgeWordData(
    val text: String,
    val partsOfSpeech: List<CambridgePartOfSpeech> = emptyList(),
)

data class CambridgePartOfSpeech(
    val text: String,
    val label: String = "",
    val ipas: List<String> = emptyList(),
    val meanings: List<CambridgeMeaning> = emptyList(),
    val phrases: List<CambridgePhrase> = emptyList(),
)

data class CambridgeMeaning(
    val explanation: String,
    val translation: String,
    val examples: List<String> = emptyList(),
)

data class CambridgePhrase(
    val text: String,
    val translation: String,
    val examples: List<String> = emptyList(),
)
