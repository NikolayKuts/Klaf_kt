package com.kuts.domain.vocabularySource

import com.kuts.domain.entities.Card
import com.kuts.domain.entities.IgnoredVocabularyWord
import com.kuts.domain.entities.VocabularySourceItem
import com.kuts.domain.entities.VocabularySourceItemCategory
import com.kuts.domain.entities.VocabularySourceItemStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val reviewCat = VocabularySourceItem(
    sourceId = 1,
    language = "en",
    foreignWord = "cat",
    nativeWord = "кот",
    category = VocabularySourceItemCategory.NEW,
    createdAt = 1,
    updatedAt = 1,
)
private val reviewCatCard = Card(deckId = 1, foreignWord = "cat", nativeWord = "кот", ipa = emptyList(), id = 7)
private val reviewIgnoredCat = IgnoredVocabularyWord("en", "cat", "кот", 1)

class VocabularySourceReviewContextTest {
    @Test
    fun cardAdditionAndDeletionAffectBothSourcesWithoutDeletingItems() {
        val items = listOf(reviewCat.copy(createdCardId = 7), reviewCat.copy(sourceId = 2))
        val added = VocabularySourceReviewContext(cards = listOf(reviewCatCard)).resolve(items)
        assertEquals(VocabularySourceItemStatus.ADDED, added[0].status)
        assertEquals(VocabularySourceItemStatus.PENDING, added[1].status)
        assertTrue(added.all { it.alreadyExists })

        val deleted = VocabularySourceReviewContext().resolve(added)
        assertEquals(2, deleted.size)
        assertTrue(deleted.all { it.status == VocabularySourceItemStatus.PENDING && !it.alreadyExists })
        assertNull(deleted[0].createdCardId)
        assertNull(deleted[0].targetDeckId)
    }

    @Test
    fun deletingOneOfTwoCardsDoesNotMakeThePairAvailable() {
        val item = reviewCat.copy(status = VocabularySourceItemStatus.ADDED, createdCardId = 7)
        val remaining = VocabularySourceReviewContext(cards = listOf(reviewCatCard.copy(id = 8, deckId = 2)))
            .resolve(listOf(item)).single()
        assertEquals(VocabularySourceItemStatus.PENDING, remaining.status)
        assertTrue(remaining.alreadyExists)
        assertNull(remaining.createdCardId)
    }

    @Test
    fun editedAndMovedLinkedCardRetainsProvenance() {
        val card = reviewCatCard.copy(deckId = 9, foreignWord = "kitten", nativeWord = "котёнок")
        val item = VocabularySourceReviewContext(cards = listOf(card))
            .resolve(listOf(reviewCat.copy(createdCardId = 7))).single()
        assertEquals(VocabularySourceItemStatus.ADDED, item.status)
        assertEquals(9, item.targetDeckId)
        assertTrue(item.alreadyExists)
    }

    @Test
    fun globalRestoreReleasesIgnoredItemsInEverySource() {
        val items = listOf(reviewCat, reviewCat.copy(sourceId = 2))
        val ignored = VocabularySourceReviewContext(ignoredWords = listOf(reviewIgnoredCat)).resolve(items)
        assertTrue(ignored.all { it.status == VocabularySourceItemStatus.IGNORED })
        val restored = VocabularySourceReviewContext().resolve(ignored)
        assertTrue(restored.all { it.status == VocabularySourceItemStatus.PENDING })
    }

    @Test
    fun unsavedDecisionOverridesIncomingGlobalRulesOnlyForTheSameMeaning() {
        val other = reviewCat.copy(nativeWord = "кошка")
        val context = VocabularySourceReviewContext(ignoredWords = listOf(reviewIgnoredCat))
        val restored = context.resolve(listOf(reviewCat, other), mapOf(reviewCat.reviewKey() to false))
        assertTrue(restored.all { it.status == VocabularySourceItemStatus.PENDING })
        val ignored = context.resolve(listOf(reviewCat, other))
        assertEquals(VocabularySourceItemStatus.IGNORED, ignored[0].status)
        assertEquals(VocabularySourceItemStatus.PENDING, ignored[1].status)
        assertEquals(VocabularySourceItemCategory.IGNORED_WORD_NEW_MEANING, ignored[1].category)
        assertFalse(ignored[1].alreadyExists)
    }

    @Test
    fun normalizedExactPairsAndLanguagesRemainIndependent() {
        val context = VocabularySourceReviewContext(ignoredWords = listOf(reviewIgnoredCat))
        val items = listOf(reviewCat.copy(foreignWord = " CAT ", nativeWord = "  КОТ "), reviewCat.copy(language = "de"))
        val result = context.resolve(items)
        assertEquals(VocabularySourceItemStatus.IGNORED, result[0].status)
        assertEquals(VocabularySourceItemStatus.PENDING, result[1].status)
    }
}
