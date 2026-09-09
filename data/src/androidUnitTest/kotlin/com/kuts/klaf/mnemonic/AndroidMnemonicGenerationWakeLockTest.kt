package com.kuts.klaf.mnemonic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AndroidMnemonicGenerationWakeLockTest {

    @Test
    fun acquireUsesSafetyTimeoutAndDoesNotAcquireTwice() {
        val handle = FakeWakeLockHandle()
        val wakeLock = AndroidMnemonicGenerationWakeLock(handle = handle)

        wakeLock.acquire()
        wakeLock.acquire()

        assertTrue(wakeLock.isHeld)
        assertEquals(1, handle.acquireCount)
        assertEquals(MNEMONIC_GENERATION_WAKE_LOCK_TIMEOUT_MILLIS, handle.acquiredTimeoutMillis)
    }

    @Test
    fun releaseReleasesHeldLockOnlyOnce() {
        val handle = FakeWakeLockHandle()
        val wakeLock = AndroidMnemonicGenerationWakeLock(handle = handle)
        wakeLock.acquire()

        wakeLock.release()
        wakeLock.release()

        assertFalse(wakeLock.isHeld)
        assertEquals(1, handle.releaseCount)
    }

    @Test
    fun releaseIsSafeAfterSystemTimeout() {
        val handle = FakeWakeLockHandle()
        val wakeLock = AndroidMnemonicGenerationWakeLock(handle = handle)
        wakeLock.acquire()
        handle.isHeld = false

        wakeLock.release()

        assertEquals(0, handle.releaseCount)
    }
}

private class FakeWakeLockHandle : MnemonicGenerationWakeLockHandle {

    override var isHeld: Boolean = false
    var acquireCount = 0
    var releaseCount = 0
    var acquiredTimeoutMillis: Long? = null

    override fun acquire(timeoutMillis: Long) {
        isHeld = true
        acquireCount++
        acquiredTimeoutMillis = timeoutMillis
    }

    override fun release() {
        isHeld = false
        releaseCount++
    }
}
