package com.kuts.klaf.common.permissions

interface INotificationPermissionManager {

    suspend fun isPermissionGranted(): Boolean

    suspend fun getPermissionState(): NotificationPermissionState

    suspend fun requestPermissionIfNeeded(): NotificationPermissionRequestResult

    fun openAppSettings()
}

enum class NotificationPermissionState {
    GRANTED,
    NOT_GRANTED,
    DENIED,
    DENIED_ALWAYS,
}

enum class NotificationPermissionRequestResult {
    GRANTED,
    DENIED,
    DENIED_ALWAYS,
}
