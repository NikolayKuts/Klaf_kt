package com.kuts.klaf.common.permissions

interface IMicrophonePermissionManager {

    /** Returns the outcome instead of throwing, so callers can react without knowing the platform. */
    suspend fun requestPermissionIfNeeded(): MicrophonePermissionRequestResult
}

enum class MicrophonePermissionRequestResult {
    GRANTED,
    DENIED,
    DENIED_ALWAYS,
}
