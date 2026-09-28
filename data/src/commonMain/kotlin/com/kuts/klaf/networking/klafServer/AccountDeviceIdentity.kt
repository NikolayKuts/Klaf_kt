package com.kuts.klaf.networking.klafServer

class AccountDeviceIdentity(
    private val provider: PersistentAccountDeviceProvider,
    private val name: String,
    private val platform: String,
) {

    suspend fun current(): AccountDevice = provider.getOrCreate(name, platform)
}
