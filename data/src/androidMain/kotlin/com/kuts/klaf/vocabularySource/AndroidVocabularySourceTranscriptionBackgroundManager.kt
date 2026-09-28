package com.kuts.klaf.vocabularySource

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.kuts.domain.managers.IVocabularySourceTranscriptionBackgroundManager
import com.kuts.domain.managers.VocabularySourceTranscriptionHandle
import com.kuts.domain.managers.VocabularySourceTranscriptionOutcome
import com.kuts.klaf.common.notifications.NotificationChannelInitializer
import com.kuts.klaf.mnemonic.AndroidApplicationVisibilityTracker
import com.kuts.klaf.mnemonic.AndroidMnemonicGenerationDiagnostics
import com.lib.lokdroid.core.logD

private data class ActiveVocabularySourceTranscription(
    val sourceId: Int,
    val sourceTitle: String,
)

class AndroidVocabularySourceTranscriptionBackgroundManager(
    context: Context,
    private val applicationVisibilityTracker: AndroidApplicationVisibilityTracker,
    private val diagnostics: AndroidMnemonicGenerationDiagnostics,
    private val notificationChannelInitializer: NotificationChannelInitializer,
    private val notifier: VocabularySourceTranscriptionNotifier,
) : IVocabularySourceTranscriptionBackgroundManager {

    private val applicationContext = context.applicationContext
    private val lock = Any()
    private val activeTranscriptions = mutableMapOf<Long, ActiveVocabularySourceTranscription>()
    private var nextRequestId = 0L

    override fun startTranscription(
        sourceId: Int,
        sourceTitle: String,
    ): VocabularySourceTranscriptionHandle = synchronized(lock) {
        val requestId = nextRequestId++
        activeTranscriptions[requestId] = ActiveVocabularySourceTranscription(
            sourceId = sourceId,
            sourceTitle = sourceTitle,
        )

        try {
            diagnostics.backgroundOperationStarted(
                operationId = requestId,
                operationName = "vocabulary-source-transcription",
            )
            notificationChannelInitializer.initializeVocabularySourceTranscriptionChannels()
            ContextCompat.startForegroundService(applicationContext, serviceIntent())
            logD(
                "Vocabulary Source transcription foreground service start requested: " +
                    "transcriptionId=$requestId, sourceId=$sourceId, activeTranscriptions=${activeTranscriptions.size}",
            )
        } catch (error: Throwable) {
            activeTranscriptions.remove(requestId)
            diagnostics.backgroundOperationFinished(
                operationId = requestId,
                operationName = "vocabulary-source-transcription",
            )
            throw error
        }

        VocabularySourceTranscriptionHandle { outcome, serverNotificationSent ->
            finishTranscription(
                requestId = requestId,
                outcome = outcome,
                serverNotificationSent = serverNotificationSent,
            )
        }
    }

    private fun finishTranscription(
        requestId: Long,
        outcome: VocabularySourceTranscriptionOutcome,
        serverNotificationSent: Boolean,
    ) {
        val finishedTranscription = synchronized(lock) {
            activeTranscriptions.remove(requestId)
        } ?: return

        logD(
            "Vocabulary Source transcription completion received: transcriptionId=$requestId, " +
                "sourceId=${finishedTranscription.sourceId}, outcome=$outcome, " +
                "serverNotificationSent=$serverNotificationSent, " +
                "appVisible=${applicationVisibilityTracker.isApplicationVisible}",
        )
        diagnostics.backgroundOperationFinished(
            operationId = requestId,
            operationName = "vocabulary-source-transcription",
        )

        try {
            if (!applicationVisibilityTracker.isApplicationVisible) {
                when (outcome) {
                    VocabularySourceTranscriptionOutcome.Succeeded -> {
                        if (serverNotificationSent) {
                            logD(
                                "Vocabulary Source transcription local success notification skipped: " +
                                    "transcriptionId=$requestId, reason=server-notification-sent",
                            )
                        } else {
                            notifier.showSuccess(
                                sourceId = finishedTranscription.sourceId,
                                sourceTitle = finishedTranscription.sourceTitle,
                            )
                        }
                    }

                    VocabularySourceTranscriptionOutcome.Failed -> {
                        notifier.showFailure(
                            sourceId = finishedTranscription.sourceId,
                            sourceTitle = finishedTranscription.sourceTitle,
                        )
                    }

                    VocabularySourceTranscriptionOutcome.Cancelled -> Unit
                }
            }
        } finally {
            synchronized(lock) {
                if (activeTranscriptions.isEmpty()) {
                    val stopRequested = applicationContext.stopService(serviceIntent())
                    logD(
                        "Vocabulary Source transcription foreground service stop requested: " +
                            "transcriptionId=$requestId, accepted=$stopRequested",
                    )
                }
            }
        }
    }

    private fun serviceIntent(): Intent =
        Intent(applicationContext, VocabularySourceTranscriptionForegroundService::class.java)
}
