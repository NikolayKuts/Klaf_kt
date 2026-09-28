package com.kuts.klaf.di

import com.kuts.klaf.server.contract.SyncHistoryItem
import com.kuts.klaf.server.contract.SyncHistoryResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.runBlocking

class AccountSyncHistoryReaderTest {

    @Test
    fun `reads history for selected account without starting synchronization`() = runBlocking {
        var requestedEmail: String? = null
        var requestedDevice: String? = null
        val entry = SyncHistoryItem("operation-a", 4L, "device-a", "EDIT_DECK", listOf("deck-a"), 123L)
        val reader = AccountSyncHistoryReader(
            selectedEmail = { "alice@example.test" },
            deviceId = { "device-a" },
            fetch = { email, device ->
                requestedEmail = email
                requestedDevice = device
                SyncHistoryResponse(4L, listOf(entry))
            },
        )

        assertEquals(listOf(entry), reader.recentHistory("alice@example.test"))
        assertEquals("alice@example.test", requestedEmail)
        assertEquals("device-a", requestedDevice)
    }

    @Test
    fun `does not query history for a different selected account`() = runBlocking {
        var fetchCount = 0
        val reader = AccountSyncHistoryReader(
            selectedEmail = { "bob@example.test" },
            deviceId = { "device-a" },
            fetch = { _, _ ->
                fetchCount++
                SyncHistoryResponse(0L, emptyList())
            },
        )

        assertFailsWith<IllegalStateException> { reader.recentHistory("alice@example.test") }
        assertEquals(0, fetchCount)
    }

    @Test
    fun `rejects history response after account switch`() = runBlocking {
        var selected = "alice@example.test"
        val reader = AccountSyncHistoryReader(
            selectedEmail = { selected },
            deviceId = { "device-a" },
            fetch = { _, _ ->
                selected = "bob@example.test"
                SyncHistoryResponse(4L, emptyList())
            },
        )

        assertFailsWith<IllegalStateException> { reader.recentHistory("alice@example.test") }
        assertEquals("bob@example.test", selected)
    }
}
