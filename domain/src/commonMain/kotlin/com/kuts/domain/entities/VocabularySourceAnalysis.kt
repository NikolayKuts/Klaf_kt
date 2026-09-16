package com.kuts.domain.entities

data class VocabularySourceAnalysis(
    val language: String,
    val items: List<VocabularySourceAnalysisItem>,
)

data class VocabularySourceAnalysisItem(
    val foreignWord: String,
    val nativeWord: String,
    val originalText: String,
    val partOfSpeech: VocabularySourceItemPartOfSpeech,
    val cefrLevel: CefrLevel?,
    val confidence: VocabularySourceItemConfidence,
    val sourceExample: String,
    val explanation: String,
    val occurrences: List<VocabularySourceItemOccurrence>,
)

