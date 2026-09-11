package com.kuts.klaf.mnemonic

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.kuts.domain.managers.IMnemonicGenerationBackgroundManager
import com.kuts.domain.managers.MnemonicGenerationHandle
import com.kuts.domain.managers.MnemonicGenerationOutcome
import com.kuts.domain.managers.MnemonicGenerationSource
import com.kuts.domain.managers.MnemonicGenerationType
import com.kuts.klaf.common.notifications.NotificationChannelInitializer
import com.lib.lokdroid.core.logD

private data class ActiveMnemonicGeneration(
    val type: MnemonicGenerationType,
    val source: MnemonicGenerationSource,
)

/** Shares one foreground service across all active mnemonic text and image requests. */
class AndroidMnemonicGenerationBackgroundManager(
    context: Context,
    private val applicationVisibilityTracker: AndroidApplicationVisibilityTracker,
    private val diagnostics: AndroidMnemonicGenerationDiagnostics,
    private val notificationChannelInitializer: NotificationChannelInitializer,
    private val notifier: MnemonicGenerationNotifier,
) : IMnemonicGenerationBackgroundManager {

    private val applicationContext = context.applicationContext
    private val lock = Any()
    private val activeGenerations = mutableMapOf<Long, ActiveMnemonicGeneration>()
    private var nextRequestId = 0L

    override fun startGeneration(
        type: MnemonicGenerationType,
        source: MnemonicGenerationSource,
    ): MnemonicGenerationHandle = synchronized(lock) {
        val requestId = nextRequestId++
        activeGenerations[requestId] = ActiveMnemonicGeneration(
            type = type,
            source = source,
        )
        diagnostics.generationStarted(generationId = requestId, type = type)

        try {
            notificationChannelInitializer.initializeMnemonicGenerationChannels()
            diagnostics.foregroundServiceStartRequested()
            ContextCompat.startForegroundService(applicationContext, serviceIntent())
            logD(
                "Mnemonic foreground service start requested: generationId=$requestId, " +
                    "type=$type, activeGenerations=${activeGenerations.size}",
            )
        } catch (error: Throwable) {
            activeGenerations.remove(requestId)
            diagnostics.foregroundServiceStartFailed()
            diagnostics.generationFinished(generationId = requestId, type = type)
            throw error
        }

        MnemonicGenerationHandle { outcome ->
            finishGeneration(requestId = requestId, outcome = outcome)
        }
    }

    private fun finishGeneration(
        requestId: Long,
        outcome: MnemonicGenerationOutcome,
    ) {
        val finishedGeneration = synchronized(lock) {
            activeGenerations.remove(requestId)
        } ?: return

        logD(
            "Mnemonic generation completion received: generationId=$requestId, " +
                "type=${finishedGeneration.type}, outcome=$outcome, " +
                "appVisible=${applicationVisibilityTracker.isApplicationVisible}",
        )

        try {
            if (!applicationVisibilityTracker.isApplicationVisible) {
                when (outcome) {
                    MnemonicGenerationOutcome.Succeeded -> {
                        notifier.showSuccess(
                            type = finishedGeneration.type,
                            source = finishedGeneration.source,
                        )
                    }

                    MnemonicGenerationOutcome.Failed -> {
                        notifier.showFailure(
                            type = finishedGeneration.type,
                            source = finishedGeneration.source,
                        )
                    }

                    MnemonicGenerationOutcome.Cancelled -> Unit
                }
            }
        } finally {
            synchronized(lock) {
                if (activeGenerations.isEmpty()) {
                    val stopRequested = applicationContext.stopService(serviceIntent())
                    diagnostics.foregroundServiceStopRequested(accepted = stopRequested)
                    logD(
                        "Mnemonic foreground service stop requested: generationId=$requestId, " +
                            "accepted=$stopRequested; ${diagnostics.snapshot()}",
                    )
                }
            }
            diagnostics.generationFinished(
                generationId = requestId,
                type = finishedGeneration.type,
            )
        }
    }

    private fun serviceIntent(): Intent {
        return Intent(applicationContext, MnemonicGenerationForegroundService::class.java)
    }
}
