package com.kuts.klaf.mnemonic

import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.Process
import android.os.SystemClock
import com.kuts.domain.managers.MnemonicGenerationType
import com.lib.lokdroid.core.logD
import com.lib.lokdroid.core.logE

private const val DIAGNOSTIC_HEARTBEAT_INTERVAL_MILLIS = 15_000L

private enum class MnemonicForegroundServiceState(
    val diagnosticName: String,
) {
    NotCreated("not-created"),
    StartRequested("start-requested"),
    Creating("creating"),
    Foreground("foreground"),
    Started("started"),
    StopRequested("stop-requested"),
    Destroying("destroying"),
    Destroyed("destroyed"),
    StartFailed("start-failed"),
}

class AndroidMnemonicGenerationDiagnostics(
    context: Context,
) {

    private val applicationContext = context.applicationContext
    private val activityManager =
        applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val connectivityManager =
        applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val powerManager =
        applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private val runtimeId = "${Process.myPid()}-${SystemClock.elapsedRealtime()}"
    private val lock = Any()
    private val activeGenerationIds = mutableSetOf<String>()
    private var networkCallbackRegistered = false
    private var networkEventSequence = 0L
    private var lastNetworkEvent = "none"
    private var serviceState = MnemonicForegroundServiceState.NotCreated
    private var serviceInstanceSequence = 0L
    private var activeServiceInstanceId: Long? = null
    private var serviceStartRequestCount = 0L
    private var serviceStartCommandCount = 0L
    private var serviceStopRequestCount = 0L
    private var heartbeatSequence = 0L

    private val heartbeat = object : Runnable {
        override fun run() {
            val heartbeatId = synchronized(lock) {
                if (activeGenerationIds.isEmpty()) {
                    return
                }
                ++heartbeatSequence
            }
            logD("Mnemonic diagnostics: generation heartbeat: id=$heartbeatId; ${snapshot()}")
            mainHandler.postDelayed(this, DIAGNOSTIC_HEARTBEAT_INTERVAL_MILLIS)
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            val eventId = recordNetworkEvent("available:$network")
            logD("Mnemonic diagnostics: network available: eventId=$eventId, network=$network; ${snapshot()}")
        }

        override fun onLosing(network: Network, maxMsToLive: Int) {
            val eventId = recordNetworkEvent("losing:$network")
            logD(
                "Mnemonic diagnostics: network losing: eventId=$eventId, network=$network, " +
                    "maxMsToLive=$maxMsToLive; ${snapshot()}",
            )
        }

        override fun onLost(network: Network) {
            val eventId = recordNetworkEvent("lost:$network")
            logD("Mnemonic diagnostics: network lost: eventId=$eventId, network=$network; ${snapshot()}")
        }

        override fun onUnavailable() {
            val eventId = recordNetworkEvent("unavailable")
            logD("Mnemonic diagnostics: network unavailable: eventId=$eventId; ${snapshot()}")
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities,
        ) {
            val eventId = recordNetworkEvent("capabilities:$network")
            logD(
                "Mnemonic diagnostics: network capabilities changed: eventId=$eventId, network=$network, " +
                    "callbackCapabilities=${networkCapabilities.diagnosticDescription()}; ${snapshot()}",
            )
        }

        override fun onBlockedStatusChanged(network: Network, blocked: Boolean) {
            val eventId = recordNetworkEvent("blocked:$network:$blocked")
            logD(
                "Mnemonic diagnostics: network blocked status changed: eventId=$eventId, " +
                    "network=$network, blocked=$blocked; ${snapshot()}",
            )
        }
    }

    fun foregroundServiceStartRequested() {
        synchronized(lock) {
            serviceStartRequestCount++
            if (
                serviceState == MnemonicForegroundServiceState.NotCreated ||
                serviceState == MnemonicForegroundServiceState.Destroyed ||
                serviceState == MnemonicForegroundServiceState.StartFailed
            ) {
                serviceState = MnemonicForegroundServiceState.StartRequested
            }
        }
    }

    fun foregroundServiceStartFailed() {
        synchronized(lock) {
            serviceState = MnemonicForegroundServiceState.StartFailed
        }
    }

    fun foregroundServiceCreating(): Long = synchronized(lock) {
        val instanceId = ++serviceInstanceSequence
        activeServiceInstanceId = instanceId
        serviceState = MnemonicForegroundServiceState.Creating
        instanceId
    }

    fun foregroundServiceEnteredForeground(instanceId: Long) {
        updateServiceState(instanceId, MnemonicForegroundServiceState.Foreground)
    }

    fun foregroundServiceStarted(instanceId: Long) {
        synchronized(lock) {
            if (activeServiceInstanceId == instanceId) {
                serviceStartCommandCount++
                serviceState = MnemonicForegroundServiceState.Started
            }
        }
    }

    fun foregroundServiceStopRequested(accepted: Boolean) {
        synchronized(lock) {
            serviceStopRequestCount++
            if (accepted) {
                serviceState = MnemonicForegroundServiceState.StopRequested
            }
        }
    }

    fun foregroundServiceDestroying(instanceId: Long) {
        updateServiceState(instanceId, MnemonicForegroundServiceState.Destroying)
    }

    fun foregroundServiceDestroyed(instanceId: Long) {
        synchronized(lock) {
            if (activeServiceInstanceId == instanceId) {
                serviceState = MnemonicForegroundServiceState.Destroyed
                activeServiceInstanceId = null
            }
        }
    }

    fun generationStarted(
        generationId: Long,
        type: MnemonicGenerationType,
    ) {
        backgroundOperationStarted(
            operationId = generationId,
            operationName = "mnemonic-$type",
        )
    }

    fun generationFinished(
        generationId: Long,
        type: MnemonicGenerationType,
    ) {
        backgroundOperationFinished(
            operationId = generationId,
            operationName = "mnemonic-$type",
        )
    }

    fun backgroundOperationStarted(
        operationId: Long,
        operationName: String,
    ) {
        val operationKey = "$operationName:$operationId"
        val wasFirstGeneration: Boolean
        val shouldRegisterCallback = synchronized(lock) {
            wasFirstGeneration = activeGenerationIds.isEmpty()
            activeGenerationIds += operationKey
            if (networkCallbackRegistered) {
                false
            } else {
                networkCallbackRegistered = true
                true
            }
        }

        if (shouldRegisterCallback) {
            registerNetworkCallback()
        }
        if (wasFirstGeneration) {
            mainHandler.postDelayed(heartbeat, DIAGNOSTIC_HEARTBEAT_INTERVAL_MILLIS)
        }
        logD(
            "Mnemonic diagnostics: background operation started: " +
                "id=$operationId, name=$operationName; " +
                snapshot(),
        )
    }

    fun backgroundOperationFinished(
        operationId: Long,
        operationName: String,
    ) {
        val operationKey = "$operationName:$operationId"
        val shouldUnregisterCallback = synchronized(lock) {
            activeGenerationIds -= operationKey
            if (activeGenerationIds.isEmpty() && networkCallbackRegistered) {
                networkCallbackRegistered = false
                true
            } else {
                false
            }
        }

        logD(
            "Mnemonic diagnostics: background operation finished: " +
                "id=$operationId, name=$operationName; " +
                snapshot(),
        )
        if (shouldUnregisterCallback) {
            mainHandler.removeCallbacks(heartbeat)
            runCatching {
                connectivityManager.unregisterNetworkCallback(networkCallback)
            }.onFailure { failure ->
                logE("Mnemonic diagnostics: network callback unregister failed: ${failure.stackTraceToString()}")
            }
        }
    }

    fun snapshot(): String {
        val processInfo = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(processInfo)
        val network = runCatching { connectivityManager.activeNetwork }.getOrNull()
        val capabilities = network?.let { activeNetwork ->
            runCatching { connectivityManager.getNetworkCapabilities(activeNetwork) }.getOrNull()
        }
        val diagnosticState = synchronized(lock) {
            "activeGenerations=${activeGenerationIds.size}, " +
                "serviceState=${serviceState.diagnosticName}, " +
                "serviceInstance=${activeServiceInstanceId ?: "none"}, " +
                "serviceStartRequests=$serviceStartRequestCount, " +
                "serviceStartCommands=$serviceStartCommandCount, " +
                "serviceStopRequests=$serviceStopRequestCount, " +
                "heartbeatCount=$heartbeatSequence, " +
                "networkCallbackRegistered=$networkCallbackRegistered, " +
                "lastNetworkEvent=$lastNetworkEvent"
        }

        return buildString {
            append("runtimeId=")
            append(runtimeId)
            append(", pid=")
            append(Process.myPid())
            append(", elapsedRealtimeMs=")
            append(SystemClock.elapsedRealtime())
            append(", processImportance=")
            append(processInfo.importance.processImportanceName())
            append(", interactive=")
            append(powerManager.isInteractive)
            append(", powerSave=")
            append(powerManager.isPowerSaveMode)
            append(", deviceIdle=")
            append(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && powerManager.isDeviceIdleMode)
            append(", ignoringBatteryOptimizations=")
            append(
                Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                    powerManager.isIgnoringBatteryOptimizations(applicationContext.packageName),
            )
            append(", backgroundRestriction=")
            append(connectivityManager.restrictBackgroundStatus.backgroundRestrictionName())
            append(", appBackgroundRestricted=")
            append(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && activityManager.isBackgroundRestricted)
            append(", ")
            append(diagnosticState)
            append(", activeNetwork=")
            append(network ?: "none")
            append(", activeNetworkMetered=")
            append(runCatching { connectivityManager.isActiveNetworkMetered }.getOrNull() ?: "unknown")
            append(", networkCapabilities=")
            append(capabilities?.diagnosticDescription() ?: "none")
        }
    }

    private fun registerNetworkCallback() {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                connectivityManager.registerDefaultNetworkCallback(networkCallback)
            } else {
                connectivityManager.registerNetworkCallback(
                    NetworkRequest.Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .build(),
                    networkCallback,
                )
            }
        }.onFailure { failure ->
            synchronized(lock) {
                networkCallbackRegistered = false
            }
            logE("Mnemonic diagnostics: network callback registration failed: ${failure.stackTraceToString()}")
        }
    }

    private fun recordNetworkEvent(event: String): Long = synchronized(lock) {
        networkEventSequence++
        lastNetworkEvent = "$networkEventSequence:$event"
        networkEventSequence
    }

    private fun updateServiceState(
        instanceId: Long,
        state: MnemonicForegroundServiceState,
    ) {
        synchronized(lock) {
            if (activeServiceInstanceId == instanceId) {
                serviceState = state
            }
        }
    }
}

private fun NetworkCapabilities.diagnosticDescription(): String {
    val transports = buildList {
        if (hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) add("wifi")
        if (hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) add("cellular")
        if (hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) add("ethernet")
        if (hasTransport(NetworkCapabilities.TRANSPORT_VPN)) add("vpn")
        if (hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)) add("bluetooth")
    }

    return "transports=${transports.ifEmpty { listOf("unknown") }}, " +
        "internet=${hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)}, " +
        "validated=${hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)}, " +
        "notRestricted=${hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)}, " +
        "notMetered=${hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)}, " +
        "notRoaming=${hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING)}, " +
        "foreground=${hasCapability(NetworkCapabilities.NET_CAPABILITY_FOREGROUND)}, " +
        "notSuspended=${
            Build.VERSION.SDK_INT < Build.VERSION_CODES.P ||
                hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED)
        }, downstreamKbps=$linkDownstreamBandwidthKbps, upstreamKbps=$linkUpstreamBandwidthKbps"
}

private fun Int.processImportanceName(): String = when (this) {
    ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND -> "foreground"
    ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE -> "foreground-service"
    ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE -> "visible"
    ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE -> "service"
    ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED -> "cached"
    else -> toString()
}

private fun Int.backgroundRestrictionName(): String = when (this) {
    ConnectivityManager.RESTRICT_BACKGROUND_STATUS_DISABLED -> "disabled"
    ConnectivityManager.RESTRICT_BACKGROUND_STATUS_WHITELISTED -> "whitelisted"
    ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED -> "enabled"
    else -> toString()
}
