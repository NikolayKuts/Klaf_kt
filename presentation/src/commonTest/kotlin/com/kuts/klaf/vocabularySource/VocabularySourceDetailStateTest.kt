package com.kuts.klaf.vocabularySource

import com.kuts.domain.entities.VocabularySourceItem
import com.kuts.domain.entities.VocabularySourceItemCategory
import kotlin.test.Test
import kotlin.test.assertEquals

class VocabularySourceDetailStateTest {
    @Test
    fun emptyNewAnalysisMustNotFallBackToPreviouslySavedItems() {
        val oldItem = VocabularySourceItem(1, foreignWord = "word", nativeWord = "meaning",
            category = VocabularySourceItemCategory.NEW, createdAt = 1, updatedAt = 1)
        val state = VocabularySourceDetailState(persistedItems = listOf(oldItem), hasNewAnalysisDraft = true)
        assertEquals(emptyList(), state.reviewItems)
    }
}
