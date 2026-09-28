package com.kuts.klaf.push

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Color
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.kuts.domain.managers.MnemonicGenerationLaunchExtras
import com.kuts.domain.managers.MnemonicGenerationSource
import com.kuts.domain.managers.MnemonicGenerationType
import com.kuts.klaf.common.notifications.NotificationChannelInitializer.Companion.MNEMONIC_GENERATION_SUCCESS_CHANNEL_ID
import com.kuts.klaf.data.R
import com.kuts.klaf.mnemonic.MnemonicGenerationNotifier
import com.kuts.klaf.vocabularySource.VocabularySourceAnalysisNotifier
import com.kuts.klaf.vocabularySource.VocabularySourceTranscriptionNotifier
import com.lib.lokdroid.core.logD
import com.lib.lokdroid.core.logE
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private const val PUSH_TYPE_KEY = "type"
private const val SOURCE_ID_KEY = "sourceId"
private const val SOURCE_TITLE_KEY = "sourceTitle"
private const val WORD_KEY = "word"
private const val VOCABULARY_SOURCE_ANALYSIS_COMPLETED_TYPE = "vocabulary_source_analysis_completed"
private const val VOCABULARY_SOURCE_TRANSCRIPTION_COMPLETED_TYPE = "vocabulary_source_transcription_completed"
private const val MNEMONIC_TEXT_COMPLETED_TYPE = "mnemonic_text_completed"
private const val MNEMONIC_IMAGE_COMPLETED_TYPE = "mnemonic_image_completed"
private const val WORD_INSIGHTS_COMPLETED_TYPE = "word_insights_completed"
private const val WORD_INSIGHTS_NOTIFICATION_ID = 43_529
private const val APP_DESTINATION_KEY = "launch_destination"
private const val APP_DECK_ID_KEY = "launch_deck_id"
private const val APP_CARD_ID_KEY = "launch_card_id"
private const val APP_DESTINATION_CARD_EDITING = "card_editing"

class KlafFirebaseMessagingService : FirebaseMessagingService(), KoinComponent {

    private val pushTokenManager: KlafPushTokenManager by inject()
    private val mnemonicGenerationNotifier: MnemonicGenerationNotifier by inject()
    private val vocabularySourceAnalysisNotifier: VocabularySourceAnalysisNotifier by inject()
    private val vocabularySourceTranscriptionNotifier: VocabularySourceTranscriptionNotifier by inject()

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        pushTokenManager.saveToken(token = token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data = message.data
        when (data[PUSH_TYPE_KEY]) {
            VOCABULARY_SOURCE_ANALYSIS_COMPLETED_TYPE -> showVocabularySourceAnalysisCompleted(data = data)
            VOCABULARY_SOURCE_TRANSCRIPTION_COMPLETED_TYPE -> showVocabularySourceTranscriptionCompleted(data = data)
            MNEMONIC_TEXT_COMPLETED_TYPE -> showMnemonicGenerationCompleted(
                data = data,
                type = MnemonicGenerationType.Text,
            )
            MNEMONIC_IMAGE_COMPLETED_TYPE -> showMnemonicGenerationCompleted(
                data = data,
                type = MnemonicGenerationType.Image,
            )
            WORD_INSIGHTS_COMPLETED_TYPE -> showWordInsightsCompleted(data = data)
            else -> logD("Klaf push message ignored: dataKeys=${data.keys}")
        }
    }

    private fun showVocabularySourceAnalysisCompleted(data: Map<String, String>) {
        val sourceId = data[SOURCE_ID_KEY]?.toIntOrNull()
        if (sourceId == null) {
            logE("Klaf push message ignored: missing vocabulary source id")
            return
        }

        vocabularySourceAnalysisNotifier.showSuccess(
            sourceId = sourceId,
            sourceTitle = data[SOURCE_TITLE_KEY].orEmpty().ifBlank { "Vocabulary source" },
        )
        logD("Klaf push vocabulary source analysis notification shown: sourceId=$sourceId")
    }

    private fun showVocabularySourceTranscriptionCompleted(data: Map<String, String>) {
        val sourceId = data[SOURCE_ID_KEY]?.toIntOrNull()
        if (sourceId == null) {
            logE("Klaf push message ignored: missing vocabulary source id")
            return
        }

        vocabularySourceTranscriptionNotifier.showSuccess(
            sourceId = sourceId,
            sourceTitle = data[SOURCE_TITLE_KEY].orEmpty().ifBlank { "Vocabulary source" },
        )
        logD("Klaf push vocabulary source transcription notification shown: sourceId=$sourceId")
    }

    private fun showMnemonicGenerationCompleted(
        data: Map<String, String>,
        type: MnemonicGenerationType,
    ) {
        val source = data.toMnemonicGenerationSource()
        if (source == null) {
            logE("Klaf push mnemonic message ignored: missing launch source")
            return
        }

        mnemonicGenerationNotifier.showSuccess(
            type = type,
            source = source,
        )
        logD("Klaf push mnemonic notification shown: type=$type")
    }

    private fun showWordInsightsCompleted(data: Map<String, String>) {
        val pendingIntent = data.toWordInsightsPendingIntent()
        if (pendingIntent == null) {
            logE("Klaf push Word Insights message ignored: missing launch context")
            return
        }

        val word = data[WORD_KEY].orEmpty().ifBlank { "word" }
        notificationManager().notify(
            WORD_INSIGHTS_NOTIFICATION_ID,
            NotificationCompat.Builder(this, MNEMONIC_GENERATION_SUCCESS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_mnemonic_image_ready_24)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Word insights are ready: $word")
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setColor(Color.GREEN)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build(),
        )
        logD("Klaf push Word Insights notification shown: word=$word")
    }

    private fun Map<String, String>.toMnemonicGenerationSource(): MnemonicGenerationSource? {
        val destination = get(MnemonicGenerationLaunchExtras.DESTINATION_KEY) ?: return null
        val deckId = get(MnemonicGenerationLaunchExtras.DECK_ID_KEY)?.toIntOrNull() ?: return null

        return when (destination) {
            MnemonicGenerationLaunchExtras.DESTINATION_CARD_ADDITION -> {
                MnemonicGenerationSource.CardCreation(deckId = deckId)
            }
            MnemonicGenerationLaunchExtras.DESTINATION_CARD_EDITING -> {
                MnemonicGenerationSource.CardEditing(
                    deckId = deckId,
                    cardId = get(MnemonicGenerationLaunchExtras.CARD_ID_KEY)?.toIntOrNull()
                        ?: return null,
                )
            }
            else -> null
        }
    }

    private fun Map<String, String>.toWordInsightsPendingIntent(): PendingIntent? {
        val deckId = get(APP_DECK_ID_KEY)?.toIntOrNull() ?: return null
        val cardId = get(APP_CARD_ID_KEY)?.toIntOrNull() ?: return null
        val launchIntent = packageManager
            .getLaunchIntentForPackage(packageName)
            ?.apply {
                action = "com.kuts.klaf.action.OPEN_WORD_INSIGHTS_RESULT"
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(APP_DESTINATION_KEY, APP_DESTINATION_CARD_EDITING)
                putExtra(APP_DECK_ID_KEY, deckId)
                putExtra(APP_CARD_ID_KEY, cardId)
            }
            ?: return null

        return PendingIntent.getActivity(
            this@KlafFirebaseMessagingService,
            WORD_INSIGHTS_NOTIFICATION_ID,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun notificationManager(): NotificationManager =
        getSystemService(NotificationManager::class.java)
}
