package com.kuts.klaf.deckList.conflictResolution

import com.kuts.domain.common.ConflictResolutionAction
import com.kuts.klaf.server.contract.SyncCard
import com.kuts.klaf.server.contract.SyncConflict
import com.kuts.klaf.server.contract.SyncDelta
import com.kuts.klaf.server.contract.SyncDeck
import com.kuts.klaf.server.contract.SyncOperation
import com.kuts.klaf.server.contract.SyncResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConflictResolutionUiModelTest {

    @Test
    fun `bulk deck choice rejects deleted or unrelated server deck`() {
        val edit = SyncOperation.EditDeck("edit-a", "deck-a", "Local")
        val conflicts = listOf(
            SyncConflict("edit-a", "CONCURRENT_CHANGE", edit, emptyList(),
                serverDeck = SyncDeck("deck-b", "Unrelated", 1L)),
            SyncConflict("edit-a", "CONCURRENT_CHANGE", edit, emptyList(),
                serverDeck = SyncDeck("deck-a", "Deleted", 1L)),
        )
        conflicts.forEachIndexed { index, conflict ->
            val model = response(conflict, deletedDecks = if (index == 1) listOf("deck-a") else emptyList())
                .toConflictResolutionUiModel()

            assertEquals(setOf(ConflictResolutionAction.ACCEPT_SERVER), model.availableActions)
            assertEquals(model.availableActions, model.conflicts.single().availableActions)
        }
    }

    @Test
    fun `bulk card choice rejects moved or unrelated server card`() {
        val edit = SyncOperation.EditCard("edit-a", "card-a", SyncCard("card-a", "deck-a", "native", "Local"))
        listOf(
            SyncCard("card-a", "deck-b", "native", "Moved"),
            SyncCard("card-b", "deck-a", "native", "Unrelated"),
        ).forEach { serverCard ->
            val model = response(SyncConflict("edit-a", "CONCURRENT_CHANGE", edit, emptyList(),
                serverCard = serverCard)).toConflictResolutionUiModel()

            assertEquals(setOf(ConflictResolutionAction.ACCEPT_SERVER), model.availableActions)
            assertEquals(model.availableActions, model.conflicts.single().availableActions)
        }
    }

    @Test
    fun `bulk card choice rejects inconsistent local payload identity`() {
        val edit = SyncOperation.EditCard("edit-a", "card-a", SyncCard("card-b", "deck-a", "native", "Local"))
        val model = response(SyncConflict("edit-a", "CONCURRENT_CHANGE", edit, emptyList(),
            serverCard = SyncCard("card-a", "deck-a", "native", "Remote"))).toConflictResolutionUiModel()

        assertEquals(setOf(ConflictResolutionAction.ACCEPT_SERVER), model.availableActions)
        assertEquals(model.availableActions, model.conflicts.single().availableActions)
    }

    @Test
    fun `bulk card choice requires every card to remain in an existing deck`() {
        val edits = listOf("deck-a", "deck-b").mapIndexed { index, deckId ->
            val card = SyncCard("card-$index", deckId, "native", "Local")
            val edit = SyncOperation.EditCard("edit-$index", card.syncId, card)
            SyncConflict(edit.operationId, "CONCURRENT_CHANGE", edit, emptyList(), serverCard = card)
        }
        val model = SyncResponse(
            revision = 2L,
            conflicts = edits,
            delta = SyncDelta(0L, 2L, emptyList(), emptyList(), listOf("deck-a"), emptyList(), emptyList()),
        ).toConflictResolutionUiModel()

        assertEquals(setOf(ConflictResolutionAction.ACCEPT_SERVER), model.availableActions)
        assertEquals(setOf(ConflictResolutionAction.ACCEPT_SERVER), model.conflicts[0].availableActions)
        assertTrue(ConflictResolutionAction.KEEP_LOCAL_CARD in model.conflicts[1].availableActions)
    }

    @Test
    fun `bulk deck choice permits a valid rename but not a blank name`() {
        listOf("Local", " ").forEach { name ->
            val edit = SyncOperation.EditDeck("edit-a", "deck-a", name)
            val model = response(SyncConflict("edit-a", "CONCURRENT_CHANGE", edit, emptyList(),
                serverDeck = SyncDeck("deck-a", "Remote", 1L))).toConflictResolutionUiModel()
            val expected = if (name.isBlank()) setOf(ConflictResolutionAction.ACCEPT_SERVER) else
                setOf(ConflictResolutionAction.ACCEPT_SERVER, ConflictResolutionAction.KEEP_LOCAL_DECK)

            assertEquals(expected, model.availableActions)
            assertEquals(expected, model.conflicts.single().availableActions)
        }
    }

    @Test
    fun `move conflict names card and both decks instead of exposing sync IDs`() {
        val move = SyncOperation.MoveCard("move-a", "card-a", "deck-a", "deck-b", 120L)
        val model = response(SyncConflict(
            operationId = "move-a",
            reason = "REVIEWED_DECK",
            localOperation = move,
            serverChanges = emptyList(),
            serverDeck = SyncDeck("deck-b", "Reviewed", 1L, reviewCount = 1),
            serverCard = SyncCard("card-a", "deck-a", "native", "word"),
        )).toConflictResolutionUiModel(
            deckNames = mapOf("deck-a" to "Source", "deck-b" to "Target"),
            cardNames = mapOf("card-a" to "word"),
        )

        assertEquals("Move card word from Source to Target", model.conflicts.single().localDescription)
        assertTrue(model.conflicts.single().serverDescription.contains("Reviewed"))
    }

    @Test
    fun `deleted deck conflict uses local deck name when server no longer has it`() {
        val edit = SyncOperation.EditCard("edit-a", "card-a", SyncCard("card-a", "deck-a", "native", "word"))
        val model = response(
            conflict = SyncConflict("edit-a", "CONCURRENT_CHANGE", edit, emptyList()),
            deletedDecks = listOf("deck-a"),
        ).toConflictResolutionUiModel(deckNames = mapOf("deck-a" to "Words"))

        assertTrue(model.conflicts.single().serverDescription.contains("deck Words and its cards"))
    }

    @Test
    fun `card removal conflict names the card from local Room`() {
        val removal = SyncOperation.DeleteCard("remove-a", "card-a")
        val model = response(SyncConflict(
            operationId = "remove-a",
            reason = "REVIEW_MEMBERSHIP_CHANGE",
            localOperation = removal,
            serverChanges = emptyList(),
            serverDeck = SyncDeck("deck-a", "Words", 1L, reviewCount = 1),
            serverCard = SyncCard("card-a", "deck-a", "native", "word"),
        )).toConflictResolutionUiModel(cardNames = mapOf("card-a" to "word"))

        assertEquals("Delete card word", model.conflicts.single().localDescription)
    }

    @Test
    fun `deleted local card uses server card name when Room row is gone`() {
        val removal = SyncOperation.DeleteCard("remove-a", "card-a")
        val model = response(SyncConflict(
            operationId = "remove-a",
            reason = "REVIEW_MEMBERSHIP_CHANGE",
            localOperation = removal,
            serverChanges = emptyList(),
            serverDeck = SyncDeck("deck-a", "Words", 1L, reviewCount = 1),
            serverCard = SyncCard("card-a", "deck-a", "native", "word"),
        )).toConflictResolutionUiModel()

        assertEquals("Delete card word", model.conflicts.single().localDescription)
    }

    @Test
    fun `deleted local deck uses server deck name when Room row is gone`() {
        val deletion = SyncOperation.DeleteDeck("delete-a", "deck-a")
        val model = response(SyncConflict(
            operationId = "delete-a",
            reason = "CONCURRENT_CHANGE",
            localOperation = deletion,
            serverChanges = emptyList(),
            serverDeck = SyncDeck("deck-a", "Words", 1L),
        )).toConflictResolutionUiModel()

        assertEquals("Delete deck Words and its cards", model.conflicts.single().localDescription)
    }

    @Test
    fun `concurrent card edit offers server or local choice`() {
        val edit = SyncOperation.EditCard("edit-a", "card-a", SyncCard("card-a", "deck-a", "native", "Local"))
        val model = response(
            conflict = SyncConflict("edit-a", "CONCURRENT_CHANGE", edit, emptyList(),
                serverCard = SyncCard("card-a", "deck-a", "native", "Remote")),
        ).toConflictResolutionUiModel()

        assertEquals(setOf(ConflictResolutionAction.ACCEPT_SERVER, ConflictResolutionAction.KEEP_LOCAL_CARD),
            model.availableActions)
        assertTrue(model.conflicts.single().localDescription.contains("Local"))
        assertTrue(model.conflicts.single().serverDescription.contains("Remote"))
    }

    @Test
    fun `card conflict exposes differing native text even when foreign text matches`() {
        val edit = SyncOperation.EditCard("edit-native", "card-a",
            SyncCard("card-a", "deck-a", "Local native", "Same foreign"))
        val model = response(
            conflict = SyncConflict("edit-native", "CONCURRENT_CHANGE", edit, emptyList(),
                serverCard = SyncCard("card-a", "deck-a", "Server native", "Same foreign")),
        ).toConflictResolutionUiModel()

        assertTrue(model.conflicts.single().localDescription.contains("Local native"))
        assertTrue(model.conflicts.single().serverDescription.contains("Server native"))
    }

    @Test
    fun `deleted deck offers complete restoration but not ordinary card rebase`() {
        val edit = SyncOperation.EditCard("edit-a", "card-a", SyncCard("card-a", "deck-a", "native", "Local"))
        val model = response(
            conflict = SyncConflict("edit-a", "CONCURRENT_CHANGE", edit, emptyList()),
            deletedDecks = listOf("deck-a"),
            deletedCards = listOf("card-a", "card-b"),
        ).toConflictResolutionUiModel()

        assertEquals(setOf(ConflictResolutionAction.ACCEPT_SERVER, ConflictResolutionAction.RESTORE_DELETED_DECK),
            model.availableActions)
        assertTrue(model.conflicts.single().serverDescription.contains("deck deck-a and its cards"))
    }

    @Test
    fun `multiple deleted decks do not attribute every deleted card to one deck`() {
        val edit = SyncOperation.EditCard("edit-a", "card-a", SyncCard("card-a", "deck-a", "native", "Local"))
        val model = response(
            conflict = SyncConflict("edit-a", "CONCURRENT_CHANGE", edit, emptyList()),
            deletedDecks = listOf("deck-a", "deck-b"),
            deletedCards = listOf("card-a", "card-b", "card-c"),
        ).toConflictResolutionUiModel()

        assertTrue(model.conflicts.single().serverDescription.contains("deck deck-a"))
        assertTrue(model.conflicts.single().serverDescription.contains("its cards"))
        assertTrue(!model.conflicts.single().serverDescription.contains("3 cards"))
    }

    @Test
    fun `separate card deletion is not counted as a deleted deck child`() {
        val edit = SyncOperation.EditCard("edit-a", "card-a", SyncCard("card-a", "deck-a", "native", "Local"))
        val model = response(
            conflict = SyncConflict("edit-a", "CONCURRENT_CHANGE", edit, emptyList()),
            deletedDecks = listOf("deck-a"),
            deletedCards = listOf("card-a", "unrelated-card"),
        ).toConflictResolutionUiModel()

        assertTrue(model.conflicts.single().serverDescription.contains("deck deck-a and its cards"))
        assertTrue(!model.conflicts.single().serverDescription.contains("2 cards"))
    }

    @Test
    fun `deleted source of a move offers card rescue`() {
        val move = SyncOperation.MoveCard("move-a", "card-a", "deck-a", "deck-b")
        val model = response(
            conflict = SyncConflict("move-a", "CONCURRENT_CHANGE", move, emptyList()),
            deletedDecks = listOf("deck-a"),
            deletedCards = listOf("card-a"),
        ).toConflictResolutionUiModel()

        assertEquals(setOf(ConflictResolutionAction.ACCEPT_SERVER, ConflictResolutionAction.RESCUE_MOVED_CARD),
            model.availableActions)
    }

    @Test
    fun `reviewed destination does not offer immediate moved card rescue`() {
        val move = SyncOperation.MoveCard("move-a", "card-a", "deck-a", "deck-b")
        val model = SyncResponse(
            revision = 2L,
            conflicts = listOf(SyncConflict("move-a", "REVIEWED_DECK", move, emptyList())),
            delta = SyncDelta(
                0L, 2L, listOf(SyncDeck("deck-b", "Reviewed", 1L, reviewCount = 1)),
                emptyList(), listOf("deck-a"), listOf("card-a"), emptyList(),
            ),
        ).toConflictResolutionUiModel()

        assertEquals(setOf(ConflictResolutionAction.ACCEPT_SERVER), model.availableActions)
    }

    @Test
    fun `reviewed move destination offers choosing another deck when server card is intact`() {
        val move = SyncOperation.MoveCard("move-a", "card-a", "deck-a", "deck-b",
            movedCardReviewDuration = 120L)
        val model = response(SyncConflict(
            operationId = "move-a",
            reason = "REVIEWED_DECK",
            localOperation = move,
            serverChanges = emptyList(),
            serverDeck = SyncDeck("deck-b", "Reviewed", 1L, reviewCount = 1),
            serverCard = SyncCard("card-a", "deck-a", "native", "word"),
        )).toConflictResolutionUiModel()

        assertEquals(setOf(ConflictResolutionAction.ACCEPT_SERVER,
            ConflictResolutionAction.RETARGET_MOVED_CARD), model.availableActions)
        assertTrue(model.conflicts.single().serverDescription.contains("reviewed"))
        assertTrue(model.conflicts.single().serverDescription.contains("another"))
    }

    @Test
    fun `older move still explains reviewed destination but cannot offer lossy retarget`() {
        val move = SyncOperation.MoveCard("move-a", "card-a", "deck-a", "deck-b")
        val model = response(SyncConflict(
            operationId = "move-a",
            reason = "REVIEWED_DECK",
            localOperation = move,
            serverChanges = emptyList(),
            serverDeck = SyncDeck("deck-b", "Reviewed", 1L, reviewCount = 1),
            serverCard = SyncCard("card-a", "deck-a", "native", "word"),
        )).toConflictResolutionUiModel()

        assertEquals(setOf(ConflictResolutionAction.ACCEPT_SERVER), model.availableActions)
        assertTrue(model.conflicts.single().serverDescription.contains("reviewed"))
    }

    @Test
    fun `review versus card removal explains the race and offers both schedule choices`() {
        val removal = SyncOperation.DeleteCard("remove-a", "card-a")
        val conflict = SyncConflict(
            operationId = "remove-a",
            reason = "REVIEW_MEMBERSHIP_CHANGE",
            localOperation = removal,
            serverChanges = emptyList(),
            serverDeck = SyncDeck("deck-a", "Words", 1L, reviewCount = 1,
                scheduledReviewDates = listOf(5_000L)),
            serverCard = SyncCard("card-a", "deck-a", "native", "word"),
        )

        val model = response(conflict).toConflictResolutionUiModel()

        assertEquals(setOf(
            ConflictResolutionAction.ACCEPT_SERVER,
            ConflictResolutionAction.KEEP_REMOVAL_RETAIN_SCHEDULE,
            ConflictResolutionAction.KEEP_REMOVAL_DUE_NOW,
        ), model.availableActions)
        assertTrue(model.conflicts.single().serverDescription.contains("review"))
        assertTrue(model.conflicts.single().serverDescription.contains("this card"))
    }

    @Test
    fun `ordinary card deletion conflict does not offer review schedule choices`() {
        val removal = SyncOperation.DeleteCard("remove-a", "card-a")
        val model = response(SyncConflict(
            operationId = "remove-a",
            reason = "CONCURRENT_CHANGE",
            localOperation = removal,
            serverChanges = emptyList(),
            serverDeck = SyncDeck("deck-a", "Words", 1L, reviewCount = 1),
            serverCard = SyncCard("card-a", "deck-a", "native", "word"),
        )).toConflictResolutionUiModel()

        assertEquals(setOf(ConflictResolutionAction.ACCEPT_SERVER), model.availableActions)
        assertEquals(setOf(ConflictResolutionAction.ACCEPT_SERVER), model.conflicts.single().availableActions)
    }

    @Test
    fun `mixed conflict types expose no unsupported bulk local action`() {
        val card = SyncOperation.EditCard("edit-a", "card-a", SyncCard("card-a", "deck-a", "native", "Local"))
        val deck = SyncOperation.EditDeck("edit-b", "deck-b", "Local deck")
        val model = SyncResponse(
            revision = 2L,
            conflicts = listOf(
                SyncConflict("edit-a", "CONCURRENT_CHANGE", card, emptyList(),
                    serverCard = SyncCard("card-a", "deck-a", "native", "Remote")),
                SyncConflict("edit-b", "CONCURRENT_CHANGE", deck, emptyList(),
                    serverDeck = SyncDeck("deck-b", "Remote deck", 1L)),
            ),
            delta = SyncDelta(0L, 2L, emptyList(), emptyList(), emptyList(), emptyList(), emptyList()),
        ).toConflictResolutionUiModel()

        assertEquals(setOf(ConflictResolutionAction.ACCEPT_SERVER), model.availableActions)
        assertEquals(2, model.conflicts.size)
        assertEquals(setOf(ConflictResolutionAction.ACCEPT_SERVER, ConflictResolutionAction.KEEP_LOCAL_CARD),
            model.conflicts[0].availableActions)
        assertEquals(setOf(ConflictResolutionAction.ACCEPT_SERVER, ConflictResolutionAction.KEEP_LOCAL_DECK),
            model.conflicts[1].availableActions)
        assertTrue(model.manualSelectionAvailable)
    }

    private fun response(
        conflict: SyncConflict,
        deletedDecks: List<String> = emptyList(),
        deletedCards: List<String> = emptyList(),
    ) = SyncResponse(
        revision = 2L,
        conflicts = listOf(conflict),
        delta = SyncDelta(0L, 2L, emptyList(), emptyList(), deletedDecks, deletedCards, emptyList()),
    )
}
