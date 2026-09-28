package com.kuts.domain.vocabularySource

import com.kuts.domain.entities.Card
import com.kuts.domain.entities.IgnoredVocabularyWord
import com.kuts.domain.entities.VocabularySourceAnalysisItem
import com.kuts.domain.entities.VocabularySourceItem
import com.kuts.domain.entities.VocabularySourceItemCategory
import com.kuts.domain.entities.VocabularySourceItemStatus

class VocabularySourceItemCategorizer {

    fun categorize(
        sourceId: Int,
        language: String,
        analysisItems: List<VocabularySourceAnalysisItem>,
        cards: List<Card>,
        ignoredWords: List<IgnoredVocabularyWord>,
        createdAt: Long,
    ): List<VocabularySourceItem> {
        val vocabularyIndex = cards
            .groupBy { card -> card.foreignWord.toVocabularyKey() }
            .mapValues { (_, cards) ->
                cards.map { card -> card.nativeWord.trim() }
                    .filter { meaning -> meaning.isNotBlank() }
                    .distinct()
            }
        val ignoredVocabularyIndex = ignoredWords
            .filter { ignoredWord -> ignoredWord.language.toLanguageKey() == language.toLanguageKey() }
            .groupBy { ignoredWord -> ignoredWord.foreignWord.toVocabularyKey() }
            .mapValues { (_, ignoredWords) ->
                ignoredWords.map { ignoredWord -> ignoredWord.nativeWord.toMeaningKey() }.distinct()
            }

        return analysisItems.mapIndexedNotNull { index, item ->
            val knownMeanings = vocabularyIndex[item.foreignWord.toVocabularyKey()].orEmpty()
            val ignoredMeanings = ignoredVocabularyIndex[item.foreignWord.toVocabularyKey()].orEmpty()
            val hasKnownMeaning = knownMeanings.any { knownMeaning ->
                knownMeaning.toMeaningKey() == item.nativeWord.toMeaningKey()
            }
            val hasIgnoredMeaning = ignoredMeanings.any { ignoredMeaning ->
                ignoredMeaning == item.nativeWord.toMeaningKey()
            }

            if (hasKnownMeaning || hasIgnoredMeaning) return@mapIndexedNotNull null

            VocabularySourceItem(
                sourceId = sourceId,
                language = language.trim(),
                foreignWord = item.foreignWord.trim(),
                transcription = item.transcription.trim(),
                nativeWord = item.nativeWord.trim(),
                originalText = item.originalText.trim(),
                partOfSpeech = item.partOfSpeech,
                cefrLevel = item.cefrLevel,
                confidence = item.confidence,
                category = when {
                    knownMeanings.isNotEmpty() -> VocabularySourceItemCategory.POSSIBLE_NEW_MEANING
                    ignoredMeanings.isNotEmpty() -> VocabularySourceItemCategory.IGNORED_WORD_NEW_MEANING
                    else -> VocabularySourceItemCategory.NEW
                },
                status = VocabularySourceItemStatus.PENDING,
                sourceExample = item.sourceExample.trim(),
                explanation = item.explanation.trim(),
                knownMeaningsSnapshot = knownMeanings.joinToString(separator = "; "),
                alreadyExists = false,
                occurrences = item.occurrences,
                firstOccurrenceOrder = item.occurrences.minOfOrNull { occurrence ->
                    occurrence.startOffset
                } ?: index,
                createdAt = createdAt,
                updatedAt = createdAt,
            )
        }
    }

}
