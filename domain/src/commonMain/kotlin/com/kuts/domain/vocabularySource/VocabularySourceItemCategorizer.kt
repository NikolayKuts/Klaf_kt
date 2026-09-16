package com.kuts.domain.vocabularySource

import com.kuts.domain.entities.Card
import com.kuts.domain.entities.VocabularySourceAnalysisItem
import com.kuts.domain.entities.VocabularySourceItem
import com.kuts.domain.entities.VocabularySourceItemCategory
import com.kuts.domain.entities.VocabularySourceItemStatus

class VocabularySourceItemCategorizer {

    fun categorize(
        sourceId: Int,
        analysisItems: List<VocabularySourceAnalysisItem>,
        cards: List<Card>,
        createdAt: Long,
    ): List<VocabularySourceItem> {
        val vocabularyIndex = cards
            .groupBy { card -> card.foreignWord.toVocabularyKey() }
            .mapValues { (_, cards) ->
                cards.map { card -> card.nativeWord.trim() }
                    .filter { meaning -> meaning.isNotBlank() }
                    .distinct()
            }

        return analysisItems.mapIndexed { index, item ->
            val knownMeanings = vocabularyIndex[item.foreignWord.toVocabularyKey()].orEmpty()
            val hasKnownMeaning = knownMeanings.any { knownMeaning ->
                knownMeaning.toMeaningKey() == item.nativeWord.toMeaningKey()
            }

            VocabularySourceItem(
                sourceId = sourceId,
                foreignWord = item.foreignWord.trim(),
                nativeWord = item.nativeWord.trim(),
                originalText = item.originalText.trim(),
                partOfSpeech = item.partOfSpeech,
                cefrLevel = item.cefrLevel,
                confidence = item.confidence,
                category = when {
                    knownMeanings.isEmpty() -> VocabularySourceItemCategory.NEW
                    hasKnownMeaning -> VocabularySourceItemCategory.NEW
                    else -> VocabularySourceItemCategory.POSSIBLE_NEW_MEANING
                },
                status = VocabularySourceItemStatus.PENDING,
                sourceExample = item.sourceExample.trim(),
                explanation = item.explanation.trim(),
                knownMeaningsSnapshot = knownMeanings.joinToString(separator = "; "),
                alreadyExists = hasKnownMeaning,
                occurrences = item.occurrences,
                firstOccurrenceOrder = item.occurrences.minOfOrNull { occurrence ->
                    occurrence.startOffset
                } ?: index,
                createdAt = createdAt,
                updatedAt = createdAt,
            )
        }
    }

    private fun String.toVocabularyKey(): String {
        return trim().lowercase()
    }

    private fun String.toMeaningKey(): String {
        return trim()
            .lowercase()
            .replace(regex = whitespaceRegex, replacement = " ")
    }

    companion object {

        private val whitespaceRegex = Regex(pattern = "\\s+")
    }
}
