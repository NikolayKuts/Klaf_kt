package com.kuts.klaf.deckList.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking

class GuestInterimDeckSelectionTest {

    @Test
    fun `returning to guest mode recreates interim deck without touching account selections`() = runBlocking {
        var guestEnsures = 0

        flowOf("alice@example.test", null, null, "bob@example.test", null)
            .ensureGuestInterimDeck({ guestEnsures++ }, { throw it })
            .toList()

        assertEquals(2, guestEnsures)
    }

    @Test
    fun `failed guest ensure does not stop observing later guest selection`() = runBlocking {
        var attempts = 0
        var failures = 0

        flowOf(null, "alice@example.test", null)
            .ensureGuestInterimDeck(
                ensureDeck = {
                    attempts++
                    if (attempts == 1) error("temporary database failure")
                },
                reportFailure = { failures++ },
            )
            .toList()

        assertEquals(2, attempts)
        assertEquals(1, failures)
    }
}
