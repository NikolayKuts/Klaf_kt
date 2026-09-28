package com.kuts.klaf.cardManagement.cardEditing

import com.kuts.domain.entities.WordMeaningInsights
import com.kuts.domain.entities.WordMeaningItem
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WordInsightsAutoSaveTest {

    @Test
    fun `empty insight response must not create a card update`() {
        assertFalse(shouldPersistAutoFetchedInsights(WordMeaningInsights.EMPTY, "originalForeignB"))
        assertFalse(shouldPersistAutoFetchedInsights(
            WordMeaningInsights(word = "originalForeignB", meanings = emptyList()),
            "originalForeignB",
        ))
        assertFalse(shouldPersistAutoFetchedInsights(
            WordMeaningInsights(word = "originalForeignB",
                meanings = listOf(WordMeaningItem(translation = " "))),
            "originalForeignB",
        ))
    }

    @Test
    fun `insights for another word must not update the card`() {
        val insights = WordMeaningInsights(
            word = "anotherWord",
            meanings = listOf(WordMeaningItem(translation = "meaning")),
        )

        assertFalse(shouldPersistAutoFetchedInsights(insights, "originalForeignB"))
    }

    @Test
    fun `valid insights for the current word may be saved`() {
        val insights = WordMeaningInsights(
            word = " originalforeignb ",
            meanings = listOf(WordMeaningItem(translation = "meaning")),
        )

        assertTrue(shouldPersistAutoFetchedInsights(insights, "originalForeignB"))
    }
}
