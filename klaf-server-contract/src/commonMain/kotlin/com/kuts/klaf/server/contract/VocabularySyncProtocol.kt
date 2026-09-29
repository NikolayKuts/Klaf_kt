package com.kuts.klaf.server.contract

import kotlinx.serialization.Serializable

/** A source and its analysis are replaced together; all links use portable sync IDs. */
@Serializable
data class SyncVocabularySource(
    val syncId: String,
    val title: String,
    val description: String,
    val url: String,
    val rawText: String,
    val cleanText: String,
    val analysisVersion: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val lastAnalyzedAt: Long? = null,
    val items: List<SyncVocabularySourceItem> = emptyList(),
    val lastChangedServerRevision: Long = 0L,
)

@Serializable
data class SyncVocabularySourceItem(
    val syncId: String,
    val language: String,
    val foreignWord: String,
    val transcription: String,
    val nativeWord: String,
    val originalText: String,
    val partOfSpeech: String,
    val cefrLevel: String? = null,
    val confidence: String,
    val category: String,
    val status: String,
    val sourceExample: String,
    val explanation: String,
    val knownMeaningsSnapshot: String,
    val alreadyExists: Boolean,
    val occurrencesJson: String,
    val createdCardSyncId: String? = null,
    val targetDeckSyncId: String? = null,
    val firstOccurrenceOrder: Int,
    val isEdited: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

/** Ignored rules are an additive set identified by their normalized language/word/meaning. */
@Serializable
data class SyncIgnoredVocabularyWord(
    val language: String,
    val foreignWord: String,
    val nativeWord: String,
    val createdAt: Long,
)
