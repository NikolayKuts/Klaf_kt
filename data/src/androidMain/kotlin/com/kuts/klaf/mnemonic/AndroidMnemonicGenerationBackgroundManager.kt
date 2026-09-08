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

private data class ActiveMnemonicGeneration(
    val type: MnemonicGenerationType,
    val source: MnemonicGenerationSource,
)

class AndroidMnemonicGenerationBackgroundManager(
    context: Context,
    private val applicationVisibilityTracker: AndroidApplicationVisibilityTracker,
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

        try {
            notificationChannelInitializer.initializeMnemonicGenerationChannels()
            ContextCompat.startForegroundService(applicationContext, serviceIntent())
        } catch (error: Throwable) {
            activeGenerations.remove(requestId)
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
                    applicationContext.stopService(serviceIntent())
                }
            }
        }
    }

    private fun serviceIntent(): Intent {
        return Intent(applicationContext, MnemonicGenerationForegroundService::class.java)
    }
}
