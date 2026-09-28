package com.kuts.domain.entities

data class VocabularySource(
    val title: String,
    val description: String = "",
    val url: String = "",
    val rawText: String = "",
    val cleanText: String = "",
    val analysisVersion: Int = CURRENT_ANALYSIS_VERSION,
    val createdAt: Long,
    val updatedAt: Long,
    val lastAnalyzedAt: Long? = null,
    val id: Int = 0,
) {

    companion object {

        const val CURRENT_ANALYSIS_VERSION = 1
        const val MAX_TITLE_LENGTH = 80
        const val MAX_TEXT_LENGTH = 40_000
    }
}

