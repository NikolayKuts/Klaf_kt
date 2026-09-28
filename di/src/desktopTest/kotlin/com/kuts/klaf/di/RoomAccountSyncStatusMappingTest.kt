package com.kuts.klaf.di

import com.kuts.klaf.deckList.common.AccountSyncIndicator
import com.kuts.klaf.room.repositoryImplementations.RoomSyncStatus
import com.kuts.klaf.room.repositoryImplementations.SyncIndicatorState
import com.kuts.klaf.server.contract.SyncEventDevice
import kotlin.test.Test
import kotlin.test.assertEquals

class RoomAccountSyncStatusMappingTest {

    @Test
    fun `registered device details survive status mapping`() {
        val source = RoomSyncStatus(
            accountEmail = "alice@example.test",
            indicator = SyncIndicatorState.YELLOW,
            confirmedRevision = 3L,
            serverRevision = 4L,
            devices = listOf(SyncEventDevice(
                id = "device-a",
                name = "Alice's Android",
                platform = "ANDROID",
                connected = true,
                lastConfirmedRevision = 3L,
                lastSuccessfulSyncAtMillis = 1_700_000_000_000L,
            )),
        )

        val result = source.toAccountSyncStatus()

        assertEquals(AccountSyncIndicator.YELLOW, result.indicator)
        assertEquals(3L, result.confirmedRevision)
        assertEquals(4L, result.serverRevision)
        assertEquals("device-a", result.devices.single().id)
        assertEquals("Alice's Android", result.devices.single().name)
        assertEquals("ANDROID", result.devices.single().platform)
        assertEquals(true, result.devices.single().connected)
        assertEquals(3L, result.devices.single().lastConfirmedRevision)
        assertEquals(1_700_000_000_000L, result.devices.single().lastSuccessfulSyncAtMillis)
    }
}
