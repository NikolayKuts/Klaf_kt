package com.kuts.klaf.common.permissions

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.lib.lokdroid.core.logE
import dev.icerock.moko.permissions.DeniedAlwaysException
import dev.icerock.moko.permissions.DeniedException
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.RequestCanceledException
import dev.icerock.moko.permissions.microphone.RECORD_AUDIO

class AndroidMokoMicrophonePermissionManager(
    context: Context,
) : IMicrophonePermissionManager, IMicrophonePermissionBinder {

    private val permissionsController = PermissionsController(
        applicationContext = context.applicationContext,
    )

    override fun bind(activity: AppCompatActivity) {
        permissionsController.bind(activity = activity)
    }

    override suspend fun requestPermissionIfNeeded(): MicrophonePermissionRequestResult {
        return try {
            permissionsController.providePermission(permission = Permission.RECORD_AUDIO)
            MicrophonePermissionRequestResult.GRANTED
        } catch (exception: DeniedAlwaysException) {
            logE("Microphone permission denied always\n${exception.stackTraceToString()}")
            MicrophonePermissionRequestResult.DENIED_ALWAYS
        } catch (exception: DeniedException) {
            logE("Microphone permission denied\n${exception.stackTraceToString()}")
            MicrophonePermissionRequestResult.DENIED
        } catch (exception: RequestCanceledException) {
            logE("Microphone permission request canceled\n${exception.stackTraceToString()}")
            MicrophonePermissionRequestResult.DENIED
        }
    }
}
