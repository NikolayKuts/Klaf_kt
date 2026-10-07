package com.kuts.klaf.deckList.common

import com.kuts.klaf.server.contract.SyncHistoryItem
import kotlin.test.Test
import kotlin.test.assertEquals

class AccountSyncHistoryTest {

    @Test
    fun `history uses registered device display name and falls back to id`() {
        val entry = SyncHistoryItem("operation-a", 4L, "device-a", "EDIT_DECK", listOf("deck-a"), 123L)
        val knownDevice = AccountSyncDevice("device-a", "My Desktop", "DESKTOP", true, 4L, 123L)

        assertEquals("My Desktop", entry.deviceName(listOf(knownDevice)))
        assertEquals("device-a", entry.deviceName(emptyList()))
    }

    @Test
    fun `history shortens technical identifiers and hides interim account email`() {
        val entry = SyncHistoryItem(
            "operation-a", 4L, "device-a", "ADD_CARD",
            listOf("4718a51d-1457-412f-9861-738e513f4e24", "account-interim:alice@example.test"),
            123L,
        )

        assertEquals("4718a51d…, interim deck", entry.affectedItemsSummary())
    }

    @Test
    fun `bulk legacy import history is summarized instead of listing every sync id`() {
        val entry = SyncHistoryItem(
            "legacy-import", 2L, "SERVER_IMPORT", "IMPORT_ANDROID_BACKUP",
            (1..1150).map { "legacy-id-$it" }, 123L,
        )

        assertEquals("1150 imported items", entry.affectedItemsSummary())
    }
}
