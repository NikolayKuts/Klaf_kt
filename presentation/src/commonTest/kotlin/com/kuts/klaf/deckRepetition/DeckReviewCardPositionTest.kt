package com.kuts.klaf.deckRepetition

import com.kuts.domain.enums.DifficultyRecallingLevel.EASY
import com.kuts.domain.enums.DifficultyRecallingLevel.GOOD
import com.kuts.domain.enums.DifficultyRecallingLevel.HARD
import kotlin.test.Test
import kotlin.test.assertEquals

class DeckReviewCardPositionTest {

    @Test
    fun `each difficulty keeps the sole card at its only valid position`() {
        assertEquals(0, calculateReviewedCardInsertionIndex(EASY, remainingCardCount = 0))
        assertEquals(0, calculateReviewedCardInsertionIndex(GOOD, remainingCardCount = 0))
        assertEquals(0, calculateReviewedCardInsertionIndex(HARD, remainingCardCount = 0))
    }

    @Test
    fun `multi-card difficulty spacing retains the existing positions`() {
        assertEquals(2, calculateReviewedCardInsertionIndex(EASY, remainingCardCount = 2))
        assertEquals(1, calculateReviewedCardInsertionIndex(GOOD, remainingCardCount = 2))
        assertEquals(1, calculateReviewedCardInsertionIndex(HARD, remainingCardCount = 2))
        assertEquals(10, calculateReviewedCardInsertionIndex(GOOD, remainingCardCount = 12))
        assertEquals(5, calculateReviewedCardInsertionIndex(HARD, remainingCardCount = 12))
    }
}
