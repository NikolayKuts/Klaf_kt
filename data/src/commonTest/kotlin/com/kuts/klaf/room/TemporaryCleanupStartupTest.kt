package com.kuts.klaf.room

import com.kuts.klaf.room.maintenance.TemporaryCleanupReceipt
import com.kuts.klaf.room.maintenance.TemporaryCleanupResult
import com.kuts.klaf.room.maintenance.TemporaryCleanupStartup
import com.kuts.klaf.room.maintenance.TemporaryCleanupStartupState
import kotlin.test.Test
import kotlin.test.assertIs
import kotlinx.coroutines.runBlocking

private val startupCleanupReceipt = TemporaryCleanupReceipt("Source", 10, 4, true)

class TemporaryCleanupStartupTest {
    @Test
    fun normalAppOpensOnlyAfterSuccessfulCleanupAndAcknowledgement() = runBlocking<Unit> {
        val startup = TemporaryCleanupStartup { TemporaryCleanupResult.Completed(startupCleanupReceipt, false) }
        startup.openApp()
        assertIs<TemporaryCleanupStartupState.Running>(startup.state.value)
        startup.run()
        assertIs<TemporaryCleanupStartupState.Report>(startup.state.value)
        startup.openApp()
        assertIs<TemporaryCleanupStartupState.Ready>(startup.state.value)
    }

    @Test
    fun cleanupOrReceiptFailureBlocksNormalAppUntilSuccessfulRetry() = runBlocking<Unit> {
        var fail = true
        val startup = TemporaryCleanupStartup {
            check(!fail) { "Receipt could not be persisted" }
            TemporaryCleanupResult.Completed(startupCleanupReceipt, false)
        }
        startup.run()
        assertIs<TemporaryCleanupStartupState.Failed>(startup.state.value)
        startup.openApp()
        assertIs<TemporaryCleanupStartupState.Failed>(startup.state.value)
        fail = false
        startup.run()
        assertIs<TemporaryCleanupStartupState.Report>(startup.state.value)
    }

    @Test
    fun ambiguousOrMissingSourceDoesNotAllowEditingInTheCleanupBuild() = runBlocking<Unit> {
        val startup = TemporaryCleanupStartup { TemporaryCleanupResult.Skipped(2) }
        startup.run()
        assertIs<TemporaryCleanupStartupState.Report>(startup.state.value)
        startup.openApp()
        assertIs<TemporaryCleanupStartupState.Report>(startup.state.value)
    }

    @Test
    fun alreadyCompletedCleanupOpensNormallyWithoutRunningAgain() = runBlocking<Unit> {
        val startup = TemporaryCleanupStartup { TemporaryCleanupResult.Completed(startupCleanupReceipt, true) }
        startup.run()
        assertIs<TemporaryCleanupStartupState.Ready>(startup.state.value)
    }
}
