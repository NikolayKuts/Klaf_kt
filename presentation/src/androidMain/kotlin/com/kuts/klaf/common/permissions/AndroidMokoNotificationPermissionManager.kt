package com.kuts.klaf.common.permissions

import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import com.kuts.klaf.common.localStore.AppLocalStore
import com.lib.lokdroid.core.logE
import dev.icerock.moko.permissions.DeniedAlwaysException
import dev.icerock.moko.permissions.DeniedException
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionState
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.notifications.REMOTE_NOTIFICATION

class AndroidMokoNotificationPermissionManager(
    context: Context,
    private val appLocalStore: AppLocalStore,
) : INotificationPermissionManager, INotificationPermissionBinder {

    private val appContext = context.applicationContext
    private val permissionsController = PermissionsController(
        applicationContext = appContext,
    )

    override fun bind(activity: AppCompatActivity) {
        permissionsController.bind(activity = activity)
    }

    override suspend fun isPermissionGranted(): Boolean {
        return permissionsController.isPermissionGranted(permission = Permission.REMOTE_NOTIFICATION)
    }

    override suspend fun getPermissionState(): NotificationPermissionState {
        val rawState = permissionsController.getPermissionState(
            permission = Permission.REMOTE_NOTIFICATION,
        )

        return when (rawState) {
            PermissionState.NotGranted -> {
                if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    && wasPermissionRequestedOnce()
                ) {
                    NotificationPermissionState.DENIED_ALWAYS
                } else {
                    NotificationPermissionState.NOT_GRANTED
                }
            }

            PermissionState.NotDetermined -> NotificationPermissionState.NOT_GRANTED
            PermissionState.Granted -> NotificationPermissionState.GRANTED
            PermissionState.DeniedAlways -> NotificationPermissionState.DENIED_ALWAYS
            PermissionState.Denied -> NotificationPermissionState.DENIED
        }
    }

    override suspend fun requestPermissionIfNeeded(): NotificationPermissionRequestResult {
        markPermissionRequestedOnce()
        return try {
            permissionsController.providePermission(permission = Permission.REMOTE_NOTIFICATION)
            NotificationPermissionRequestResult.GRANTED
        } catch (exception: DeniedAlwaysException) {
            logE("Notification permission denied always\n${exception.stackTraceToString()}")
            NotificationPermissionRequestResult.DENIED_ALWAYS
        } catch (exception: DeniedException) {
            logE("Notification permission denied\n${exception.stackTraceToString()}")
            NotificationPermissionRequestResult.DENIED
        }
    }

    override fun openAppSettings() {
        permissionsController.openAppSettings()
    }

    private fun wasPermissionRequestedOnce(): Boolean {
        return appLocalStore.notificationPermissionRequestedOnce
    }

    private fun markPermissionRequestedOnce() {
        appLocalStore.notificationPermissionRequestedOnce = true
    }
}
