package com.kuts.klaf.vocabularySource

import com.kuts.domain.entities.VocabularySourceItem
import com.kuts.domain.entities.VocabularySourceItemCategory
import com.kuts.domain.entities.VocabularySourceItemStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class VocabularySourceDetailStateTest {
    @Test
    fun ignoredItemsFollowPendingItemsWithoutChangingSelectionOrSourceIndexes() {
        val items = listOf(
            VocabularySourceItemStatus.IGNORED,
            VocabularySourceItemStatus.PENDING,
            VocabularySourceItemStatus.ADDED,
            VocabularySourceItemStatus.IGNORED,
            VocabularySourceItemStatus.PENDING,
        ).mapIndexed { index, status ->
            VocabularySourceItem(
                sourceId = 1,
                foreignWord = "word$index",
                nativeWord = "meaning",
                category = VocabularySourceItemCategory.NEW,
                status = status,
                createdAt = 1,
                updatedAt = 1,
            )
        }
        for (state in listOf(
            VocabularySourceDetailState(persistedItems = items, selectedItemIndexes = setOf(0, 4)),
            VocabularySourceDetailState(draftItems = items, selectedItemIndexes = setOf(0, 4)),
        )) {
            assertEquals(listOf(1, 4, 0, 3), state.itemHolders.map { it.sourceIndex })
            assertEquals(listOf(4, 0), state.itemHolders.filter { it.isSelected }.map { it.sourceIndex })
            assertEquals(items, state.reviewItems)
            val originalOrder = state.copy(ignoredItemsLast = false)
            assertEquals(listOf(0, 1, 3, 4), originalOrder.itemHolders.map { it.sourceIndex })
            assertEquals(setOf(0, 4), originalOrder.selectedItemIndexes)
            assertEquals(state.selectedItemCount, originalOrder.selectedItemCount)
            assertEquals(state.selectedItemsForAdding, originalOrder.selectedItemsForAdding)
        }
    }

    @Test
    fun reviewActionsAreAvailableOnlyForApplicableSelectedItems() {
        val items = listOf(
            VocabularySourceItemStatus.PENDING,
            VocabularySourceItemStatus.IGNORED,
            VocabularySourceItemStatus.ADDED,
        ).map { status ->
            VocabularySourceItem(
                sourceId = 1,
                foreignWord = status.name,
                nativeWord = "meaning",
                category = VocabularySourceItemCategory.NEW,
                status = status,
                createdAt = 1,
                updatedAt = 1,
            )
        }
        val selectionCases = listOf(
            Triple(emptySet<Int>(), false, false),
            Triple(setOf(0), true, false),
            Triple(setOf(1), false, true),
            Triple(setOf(0, 1), true, true),
            Triple(setOf(2, 99), false, false),
        )

        for ((selection, canIgnore, canRestore) in selectionCases) {
            val persistedState = VocabularySourceDetailState(
                persistedItems = items,
                selectedItemIndexes = selection,
            )
            val draftState = VocabularySourceDetailState(
                persistedItems = items.reversed(),
                draftItems = items,
                selectedItemIndexes = selection,
                hasNewAnalysisDraft = true,
            )
            for (state in listOf(persistedState, draftState)) {
                assertEquals(canIgnore, state.hasSelectedPendingItems, "Ignore for selection=$selection")
                assertEquals(canRestore, state.hasSelectedIgnoredItems, "Restore for selection=$selection")
            }
        }
    }

    @Test
    fun emptyNewAnalysisMustNotFallBackToPreviouslySavedItems() {
        val oldItem = VocabularySourceItem(1, foreignWord = "word", nativeWord = "meaning",
            category = VocabularySourceItemCategory.NEW, createdAt = 1, updatedAt = 1)
        val state = VocabularySourceDetailState(persistedItems = listOf(oldItem), hasNewAnalysisDraft = true)
        assertEquals(emptyList(), state.reviewItems)
    }
}
