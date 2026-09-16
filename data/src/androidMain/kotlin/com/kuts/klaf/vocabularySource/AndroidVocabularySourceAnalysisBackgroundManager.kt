package com.kuts.klaf.vocabularySource

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.kuts.domain.managers.IVocabularySourceAnalysisBackgroundManager
import com.kuts.domain.managers.VocabularySourceAnalysisHandle
import com.kuts.domain.managers.VocabularySourceAnalysisOutcome
import com.kuts.klaf.common.notifications.NotificationChannelInitializer
import com.kuts.klaf.mnemonic.AndroidApplicationVisibilityTracker
import com.kuts.klaf.mnemonic.AndroidMnemonicGenerationDiagnostics
import com.lib.lokdroid.core.logD

private data class ActiveVocabularySourceAnalysis(
    val sourceId: Int,
    val sourceTitle: String,
)

class AndroidVocabularySourceAnalysisBackgroundManager(
    context: Context,
    private val applicationVisibilityTracker: AndroidApplicationVisibilityTracker,
    private val diagnostics: AndroidMnemonicGenerationDiagnostics,
    private val notificationChannelInitializer: NotificationChannelInitializer,
    private val notifier: VocabularySourceAnalysisNotifier,
) : IVocabularySourceAnalysisBackgroundManager {

    private val applicationContext = context.applicationContext
    private val lock = Any()
    private val activeAnalyses = mutableMapOf<Long, ActiveVocabularySourceAnalysis>()
    private var nextRequestId = 0L

    override fun startAnalysis(
        sourceId: Int,
        sourceTitle: String,
    ): VocabularySourceAnalysisHandle = synchronized(lock) {
        val requestId = nextRequestId++
        activeAnalyses[requestId] = ActiveVocabularySourceAnalysis(
            sourceId = sourceId,
            sourceTitle = sourceTitle,
        )

        try {
            diagnostics.backgroundOperationStarted(
                operationId = requestId,
                operationName = "vocabulary-source-analysis",
            )
            notificationChannelInitializer.initializeVocabularySourceAnalysisChannels()
            ContextCompat.startForegroundService(applicationContext, serviceIntent())
            logD(
                "Vocabulary Source analysis foreground service start requested: " +
                    "analysisId=$requestId, sourceId=$sourceId, activeAnalyses=${activeAnalyses.size}",
            )
        } catch (error: Throwable) {
            activeAnalyses.remove(requestId)
            diagnostics.backgroundOperationFinished(
                operationId = requestId,
                operationName = "vocabulary-source-analysis",
            )
            throw error
        }

        VocabularySourceAnalysisHandle { outcome ->
            finishAnalysis(requestId = requestId, outcome = outcome)
        }
    }

    private fun finishAnalysis(
        requestId: Long,
        outcome: VocabularySourceAnalysisOutcome,
    ) {
        val finishedAnalysis = synchronized(lock) {
            activeAnalyses.remove(requestId)
        } ?: return

        logD(
            "Vocabulary Source analysis completion received: analysisId=$requestId, " +
                "sourceId=${finishedAnalysis.sourceId}, outcome=$outcome, " +
                "appVisible=${applicationVisibilityTracker.isApplicationVisible}",
        )
        diagnostics.backgroundOperationFinished(
            operationId = requestId,
            operationName = "vocabulary-source-analysis",
        )

        try {
            if (!applicationVisibilityTracker.isApplicationVisible) {
                when (outcome) {
                    VocabularySourceAnalysisOutcome.Succeeded -> {
                        notifier.showSuccess(
                            sourceId = finishedAnalysis.sourceId,
                            sourceTitle = finishedAnalysis.sourceTitle,
                        )
                    }

                    VocabularySourceAnalysisOutcome.Failed -> {
                        notifier.showFailure(
                            sourceId = finishedAnalysis.sourceId,
                            sourceTitle = finishedAnalysis.sourceTitle,
                        )
                    }

                    VocabularySourceAnalysisOutcome.Cancelled -> Unit
                }
            }
        } finally {
            synchronized(lock) {
                if (activeAnalyses.isEmpty()) {
                    val stopRequested = applicationContext.stopService(serviceIntent())
                    logD(
                        "Vocabulary Source analysis foreground service stop requested: " +
                            "analysisId=$requestId, accepted=$stopRequested",
                    )
                }
            }
        }
    }

    private fun serviceIntent(): Intent =
        Intent(applicationContext, VocabularySourceAnalysisForegroundService::class.java)
}
