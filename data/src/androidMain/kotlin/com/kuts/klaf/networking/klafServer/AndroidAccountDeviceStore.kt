package com.kuts.klaf.networking.klafServer

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val PREFERENCES_NAME = "klaf_account_device"
private const val ACCOUNT_DEVICE_KEY = "account_device"

class AndroidAccountDeviceStore(context: Context) : AccountDeviceStore {

    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun read(): AccountDevice? = preferences.getString(ACCOUNT_DEVICE_KEY, null)?.let { value ->
        Json.decodeFromString<AccountDevice>(value)
    }

    override fun write(device: AccountDevice) {
        check(preferences.edit().putString(ACCOUNT_DEVICE_KEY, Json.encodeToString(device)).commit()) {
            "Unable to persist account device"
        }
    }
}
