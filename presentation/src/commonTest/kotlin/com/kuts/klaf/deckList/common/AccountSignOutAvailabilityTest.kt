package com.kuts.klaf.deckList.common

import com.kuts.domain.common.IDataSynchronizationState
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AccountSignOutAvailabilityTest {

    @Test
    fun `account sign out is disabled throughout manual synchronization`() {
        assertFalse(accountSignOutAllowed(
            accountMode = true,
            synchronizationState = IDataSynchronizationState.Synchronizing("Sending changes"),
            status = AccountSyncStatus(),
        ))
        assertFalse(accountSignOutAllowed(
            accountMode = true,
            synchronizationState = IDataSynchronizationState.Initial,
            status = AccountSyncStatus(indicator = AccountSyncIndicator.SYNCING),
        ))
    }

    @Test
    fun `account sign out is restored after synchronization ends`() {
        assertTrue(accountSignOutAllowed(
            accountMode = true,
            synchronizationState = IDataSynchronizationState.Failed,
            status = AccountSyncStatus(indicator = AccountSyncIndicator.RED),
        ))
        assertTrue(accountSignOutAllowed(
            accountMode = false,
            synchronizationState = IDataSynchronizationState.Synchronizing("Legacy sync"),
            status = AccountSyncStatus(indicator = AccountSyncIndicator.SYNCING),
        ))
    }
}
