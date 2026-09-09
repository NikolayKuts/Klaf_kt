package com.kuts.klaf.mnemonic

import android.content.Context
import android.os.PowerManager
import kotlin.time.Duration.Companion.minutes

private const val WAKE_LOCK_TAG = "com.kuts.klaf:mnemonic-generation"
internal val MNEMONIC_GENERATION_WAKE_LOCK_TIMEOUT_MILLIS = 10.minutes.inWholeMilliseconds

internal interface MnemonicGenerationWakeLockHandle {

    val isHeld: Boolean

    fun acquire(timeoutMillis: Long)

    fun release()
}

private class AndroidWakeLockHandle(
    private val wakeLock: PowerManager.WakeLock,
) : MnemonicGenerationWakeLockHandle {

    override val isHeld: Boolean get() = wakeLock.isHeld

    override fun acquire(timeoutMillis: Long) {
        wakeLock.acquire(timeoutMillis)
    }

    override fun release() {
        wakeLock.release()
    }
}

internal class AndroidMnemonicGenerationWakeLock(
    private val handle: MnemonicGenerationWakeLockHandle,
    private val timeoutMillis: Long = MNEMONIC_GENERATION_WAKE_LOCK_TIMEOUT_MILLIS,
) {

    constructor(context: Context) : this(
        handle =
            AndroidWakeLockHandle(
                wakeLock =
                    (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
                        .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)
                        .apply { setReferenceCounted(false) },
            ),
    )

    val isHeld: Boolean get() = handle.isHeld

    fun acquire() {
        if (!handle.isHeld) {
            handle.acquire(timeoutMillis)
        }
    }

    fun release() {
        if (handle.isHeld) {
            handle.release()
        }
    }
}
