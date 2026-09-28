package com.kuts.domain.entities

import kotlinx.serialization.Serializable

data class VocabularySourceItem(
    val sourceId: Int,
    val language: String = "",
    val foreignWord: String,
    val transcription: String = "",
    val nativeWord: String,
    val originalText: String = "",
    val partOfSpeech: VocabularySourceItemPartOfSpeech = VocabularySourceItemPartOfSpeech.UNKNOWN,
    val cefrLevel: CefrLevel? = null,
    val confidence: VocabularySourceItemConfidence = VocabularySourceItemConfidence.MEDIUM,
    val category: VocabularySourceItemCategory,
    val status: VocabularySourceItemStatus = VocabularySourceItemStatus.PENDING,
    val sourceExample: String = "",
    val explanation: String = "",
    val knownMeaningsSnapshot: String = "",
    val alreadyExists: Boolean = false,
    val occurrences: List<VocabularySourceItemOccurrence> = emptyList(),
    val createdCardId: Int? = null,
    val targetDeckId: Int? = null,
    val firstOccurrenceOrder: Int = 0,
    val isEdited: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val id: Int = 0,
)

enum class VocabularySourceItemStatus {
    PENDING,
    ADDED,
    IGNORED,
}

enum class VocabularySourceItemCategory {
    NEW,
    POSSIBLE_NEW_MEANING,
    IGNORED_WORD_NEW_MEANING,
}

enum class VocabularySourceItemConfidence {
    LOW,
    MEDIUM,
    HIGH,
}

enum class VocabularySourceItemPartOfSpeech {
    UNKNOWN,
    NOUN,
    VERB,
    ADJECTIVE,
    ADVERB,
    PHRASAL_VERB,
    PHRASE,
    IDIOM,
    OTHER,
}

@Serializable
data class VocabularySourceItemOccurrence(
    val timestamp: String = "",
    val startOffset: Int = 0,
    val endOffset: Int = 0,
    val sentence: String = "",
)
