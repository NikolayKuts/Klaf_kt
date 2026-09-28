package com.kuts.klaf.cardTransferring.common

import com.kuts.domain.entities.Deck
import kotlin.test.Test
import kotlin.test.assertEquals

class CardMoveTargetSelectionTest {

    @Test
    fun `picker excludes the source and every reviewed destination`() {
        val source = deck(id = 1, reviewCount = 0)
        val reviewed = deck(id = 2, reviewCount = 1)
        val reviewedAgain = deck(id = 3, reviewCount = 6)
        val available = deck(id = 4, reviewCount = 0)

        val targets = listOf(source, reviewed, available, reviewedAgain)
            .availableMoveTargets(sourceDeckId = source.id)

        assertEquals(listOf(available), targets)
    }

    @Test
    fun `picker has no target when all other decks have been reviewed`() {
        val source = deck(id = 1, reviewCount = 0)
        val reviewed = deck(id = 2, reviewCount = 1)

        assertEquals(
            emptyList(),
            listOf(source, reviewed).availableMoveTargets(sourceDeckId = source.id),
        )
    }

    private fun deck(id: Int, reviewCount: Int): Deck = Deck(
        id = id,
        name = "deck-$id",
        creationDate = 1_000L,
        reviewCount = reviewCount,
    )
}
