package com.kuts.klaf.networking.klafServer

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface AccountDeviceStore {

    fun read(): AccountDevice?

    fun write(device: AccountDevice)
}

/** One installation ID is reused across accounts and app restarts. */
class PersistentAccountDeviceProvider(private val store: AccountDeviceStore) {

    private val mutex = Mutex()

    suspend fun getOrCreate(name: String, platform: String): AccountDevice = mutex.withLock {
        store.read()?.let { saved -> return@withLock saved }
        val normalizedName = name.trim()
        require(normalizedName.isNotEmpty()) { "Device name is required" }
        require(platform == "ANDROID" || platform == "DESKTOP") { "Unsupported device platform" }
        AccountDevice(newAccountProtocolId(), normalizedName, platform).also(store::write)
    }
}
