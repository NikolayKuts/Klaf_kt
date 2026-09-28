package com.kuts.klaf.di

internal fun useAccountScopedStorage(
    requestedMode: String?,
    isolatedTestIdentity: Boolean,
    legacyDatabaseExists: Boolean,
): Boolean {
    if (isolatedTestIdentity) return true

    return when (requestedMode?.trim()?.lowercase().orEmpty().ifEmpty { "room" }) {
        "legacy" -> false
        "room" -> {
            check(!legacyDatabaseExists) {
                "Room/REST storage requires a clean installation; legacy klaf_kt.db is still present"
            }
            true
        }
        else -> throw IllegalArgumentException("klaf.client.storage.mode must be legacy or room")
    }
}
