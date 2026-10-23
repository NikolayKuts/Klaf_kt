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

        val result = source.toAccountSyncStatus(currentDeviceId = "device-a")

        assertEquals(AccountSyncIndicator.YELLOW, result.indicator)
        assertEquals(3L, result.confirmedRevision)
        assertEquals(4L, result.serverRevision)
        assertEquals("device-a", result.devices.single().id)
        assertEquals("Alice's Android", result.devices.single().name)
        assertEquals("ANDROID", result.devices.single().platform)
        assertEquals(true, result.devices.single().connected)
        assertEquals(3L, result.devices.single().lastConfirmedRevision)
        assertEquals(1_700_000_000_000L, result.devices.single().lastSuccessfulSyncAtMillis)
        assertEquals(true, result.devices.single().isCurrentDevice)
    }

    @Test
    fun `current offline device is pinned before other connected and offline devices`() {
        val source = RoomSyncStatus(devices = listOf(
            device("offline-a", connected = false),
            device("connected-a", connected = true),
            device("current", connected = false),
            device("offline-b", connected = false),
            device("connected-b", connected = true),
        ))

        val result = source.toAccountSyncStatus(currentDeviceId = "current")

        assertEquals(listOf("current", "connected-a", "connected-b", "offline-a", "offline-b"), result.devices.map { it.id })
        assertEquals(listOf("current"), result.devices.filter { it.isCurrentDevice }.map { it.id })
        assertEquals(false, result.devices.first().connected)
    }

    @Test
    fun `missing or unknown current device does not mark another connected device`() {
        val source = RoomSyncStatus(devices = listOf(
            device("offline", connected = false),
            device("connected", connected = true),
        ))

        for (currentDeviceId in listOf(null, "missing")) {
            val result = source.toAccountSyncStatus(currentDeviceId)

            assertEquals(listOf("connected", "offline"), result.devices.map { it.id })
            assertEquals(false, result.devices.any { it.isCurrentDevice })
        }
        assertEquals(emptyList(), RoomSyncStatus().toAccountSyncStatus("current").devices)
    }

    private fun device(id: String, connected: Boolean): SyncEventDevice = SyncEventDevice(
        id = id,
        name = "Same display name",
        platform = "DESKTOP",
        connected = connected,
        lastConfirmedRevision = null,
        lastSuccessfulSyncAtMillis = null,
    )
}
