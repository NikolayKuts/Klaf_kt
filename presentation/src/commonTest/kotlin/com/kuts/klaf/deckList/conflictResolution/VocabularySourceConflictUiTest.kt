package com.kuts.klaf.deckList.conflictResolution

import com.kuts.domain.common.ConflictResolutionAction
import com.kuts.klaf.server.contract.SyncConflict
import com.kuts.klaf.server.contract.SyncDelta
import com.kuts.klaf.server.contract.SyncOperation
import com.kuts.klaf.server.contract.SyncResponse
import com.kuts.klaf.server.contract.SyncVocabularySource
import com.kuts.klaf.server.contract.SyncVocabularySourceItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun conflictSource(title: String, word: String) = SyncVocabularySource("source-a", title, "", "",
    "raw $word", "clean $word", 1, 1, 2, null, listOf(SyncVocabularySourceItem("item-a", "en", word, "",
        "meaning", "", "UNKNOWN", null, "MEDIUM", "NEW", "PENDING", "", "", "", false, "[]", null, null, 0, false, 1, 2)))

private fun sourceConflictResponse(reason: String, serverPresent: Boolean = true): SyncResponse {
    val operation = SyncOperation.UpsertVocabularySource("op-a", conflictSource("Same title", "localword"))
    val source = if (serverPresent) conflictSource("Same title", "serverword") else null
    return SyncResponse(2L, conflicts = listOf(SyncConflict("op-a", reason, operation, emptyList(), serverSource = source)),
        delta = SyncDelta(1L, 2L, emptyList(), emptyList(), emptyList(), emptyList(), emptyList()))
}

class VocabularySourceConflictUiTest {

    @Test
    fun `same title and word count still show enough details to distinguish the two analyses`() {
        val model = sourceConflictResponse("CONCURRENT_CHANGE").toConflictResolutionUiModel()
        assertTrue(model.conflicts.single().localDescription.contains("localword"))
        assertTrue(model.conflicts.single().serverDescription.contains("serverword"))
        assertEquals(setOf(ConflictResolutionAction.ACCEPT_SERVER, ConflictResolutionAction.KEEP_LOCAL_SOURCE), model.availableActions)
    }

    @Test
    fun `deleted source explains absence and offers retaining local analysis`() {
        val model = sourceConflictResponse("CONCURRENT_CHANGE", serverPresent = false).toConflictResolutionUiModel()
        assertTrue(model.conflicts.single().serverDescription.contains("deleted"))
        assertTrue(ConflictResolutionAction.KEEP_LOCAL_SOURCE in model.availableActions)
    }

    @Test
    fun `missing card or deck links do not offer a keep local action that cannot succeed`() {
        val model = sourceConflictResponse("SOURCE_LINK_MISSING").toConflictResolutionUiModel()
        assertEquals(setOf(ConflictResolutionAction.ACCEPT_SERVER), model.availableActions)
        assertEquals(model.availableActions, model.conflicts.single().availableActions)
    }
}
