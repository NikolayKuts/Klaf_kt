package com.kuts.domain.vocabularySource

import com.kuts.domain.entities.Card
import com.kuts.domain.entities.VocabularySourceAnalysisItem
import com.kuts.domain.entities.VocabularySourceItemCategory
import com.kuts.domain.entities.VocabularySourceItemConfidence
import com.kuts.domain.entities.VocabularySourceItemOccurrence
import com.kuts.domain.entities.VocabularySourceItemPartOfSpeech
import com.kuts.domain.entities.VocabularySourceItemStatus
import com.kuts.domain.entities.WordMeaningInsights
import kotlin.test.Test
import kotlin.test.assertEquals

class VocabularySourceItemCategorizerTest {

    private val categorizer = VocabularySourceItemCategorizer()

    @Test
    fun `categorize marks absent word as new pending`() {
        val result = categorizer.categorize(
            sourceId = 1,
            analysisItems = listOf(analysisItem(foreignWord = "severance", nativeWord = "разделение")),
            cards = emptyList(),
            createdAt = 100L,
        )

        assertEquals(expected = VocabularySourceItemCategory.NEW, actual = result.single().category)
        assertEquals(expected = VocabularySourceItemStatus.PENDING, actual = result.single().status)
    }

    @Test
    fun `categorize marks known word with different meaning as possible new meaning`() {
        val result = categorizer.categorize(
            sourceId = 1,
            analysisItems = listOf(analysisItem(foreignWord = "charge", nativeWord = "заряжать")),
            cards = listOf(card(foreignWord = "charge", nativeWord = "обвинение")),
            createdAt = 100L,
        )

        assertEquals(
            expected = VocabularySourceItemCategory.POSSIBLE_NEW_MEANING,
            actual = result.single().category,
        )
        assertEquals(expected = VocabularySourceItemStatus.PENDING, actual = result.single().status)
        assertEquals(expected = "обвинение", actual = result.single().knownMeaningsSnapshot)
    }

    @Test
    fun `categorize marks exact known meaning as already existing`() {
        val result = categorizer.categorize(
            sourceId = 1,
            analysisItems = listOf(analysisItem(foreignWord = "charge", nativeWord = "плата")),
            cards = listOf(card(foreignWord = "Charge", nativeWord = "  плата  ")),
            createdAt = 100L,
        )

        assertEquals(expected = VocabularySourceItemCategory.NEW, actual = result.single().category)
        assertEquals(expected = VocabularySourceItemStatus.PENDING, actual = result.single().status)
        assertEquals(expected = true, actual = result.single().alreadyExists)
    }

    @Test
    fun `categorize uses first occurrence offset as order`() {
        val result = categorizer.categorize(
            sourceId = 1,
            analysisItems = listOf(
                analysisItem(
                    foreignWord = "go",
                    nativeWord = "идти",
                    occurrences = listOf(
                        VocabularySourceItemOccurrence(startOffset = 30, endOffset = 32),
                        VocabularySourceItemOccurrence(startOffset = 10, endOffset = 12),
                    ),
                ),
            ),
            cards = emptyList(),
            createdAt = 100L,
        )

        assertEquals(expected = 10, actual = result.single().firstOccurrenceOrder)
    }

    private fun analysisItem(
        foreignWord: String,
        nativeWord: String,
        occurrences: List<VocabularySourceItemOccurrence> = listOf(
            VocabularySourceItemOccurrence(
                startOffset = 0,
                endOffset = foreignWord.length,
                sentence = "$foreignWord example",
            ),
        ),
    ): VocabularySourceAnalysisItem {
        return VocabularySourceAnalysisItem(
            foreignWord = foreignWord,
            nativeWord = nativeWord,
            originalText = foreignWord,
            partOfSpeech = VocabularySourceItemPartOfSpeech.NOUN,
            cefrLevel = null,
            confidence = VocabularySourceItemConfidence.HIGH,
            sourceExample = "$foreignWord example",
            explanation = "test explanation",
            occurrences = occurrences,
        )
    }

    private fun card(
        foreignWord: String,
        nativeWord: String,
    ): Card {
        return Card(
            deckId = 1,
            nativeWord = nativeWord,
            foreignWord = foreignWord,
            ipa = emptyList(),
            wordMeaningInsights = WordMeaningInsights.EMPTY,
        )
    }
}
