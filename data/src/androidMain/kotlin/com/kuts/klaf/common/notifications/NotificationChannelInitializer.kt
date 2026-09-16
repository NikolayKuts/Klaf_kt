package com.kuts.klaf.common.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.graphics.Color
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import com.kuts.klaf.data.R

class NotificationChannelInitializer(
    private val context: Context,
    private val notificationManager: NotificationManager,
) {

    companion object {

        const val WORK_LOGIC_NOTIFICATION_CHANNEL_NAME = "Work logic channel"
        const val WORK_LOGIC_NOTIFICATION_CHANNEL_ID = "work_logic_channel_id"
        const val DECK_REPETITION_CHANNEL_NAME = "Deck repetition channel"
        const val DECK_REPETITION_CHANNEL_ID = "deck_repetition_channel_id"
        // Channel IDs cannot be renamed after creation without losing the user's settings.
        const val MNEMONIC_GENERATION_PROGRESS_CHANNEL_ID = "mnemonic_image_progress_channel_id"
        const val MNEMONIC_GENERATION_SUCCESS_CHANNEL_ID = "mnemonic_image_success_alert_channel_id"
        const val MNEMONIC_GENERATION_FAILURE_CHANNEL_ID = "mnemonic_image_failure_channel_id"
        const val VOCABULARY_SOURCE_ANALYSIS_PROGRESS_CHANNEL_ID = "vocabulary_source_analysis_progress_channel_id"
        const val VOCABULARY_SOURCE_ANALYSIS_SUCCESS_CHANNEL_ID = "vocabulary_source_analysis_success_channel_id"
        const val VOCABULARY_SOURCE_ANALYSIS_FAILURE_CHANNEL_ID = "vocabulary_source_analysis_failure_channel_id"
    }

    fun initialize() {
        createDeckRepetitionChannel()
        createWorkLogicChannel()
        initializeMnemonicGenerationChannels()
        initializeVocabularySourceAnalysisChannels()
    }

    fun initializeMnemonicGenerationChannels() {
        createMnemonicGenerationProgressChannel()
        createMnemonicGenerationSuccessChannel()
        createMnemonicGenerationFailureChannel()
    }

    fun initializeVocabularySourceAnalysisChannels() {
        createVocabularySourceAnalysisProgressChannel()
        createVocabularySourceAnalysisSuccessChannel()
        createVocabularySourceAnalysisFailureChannel()
    }

    private fun createMnemonicGenerationProgressChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                MNEMONIC_GENERATION_PROGRESS_CHANNEL_ID,
                context.getString(R.string.mnemonic_generation_progress_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(R.string.mnemonic_generation_progress_channel_description)
                setSound(null, null)
                enableVibration(false)
            }

            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    private fun createMnemonicGenerationSuccessChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                MNEMONIC_GENERATION_SUCCESS_CHANNEL_ID,
                context.getString(R.string.mnemonic_generation_success_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.mnemonic_generation_success_channel_description)
                lightColor = Color.GREEN
                enableLights(true)
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    private fun createMnemonicGenerationFailureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                MNEMONIC_GENERATION_FAILURE_CHANNEL_ID,
                context.getString(R.string.mnemonic_generation_failure_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.mnemonic_generation_failure_channel_description)
                lightColor = Color.RED
                enableLights(true)
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    private fun createVocabularySourceAnalysisProgressChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                VOCABULARY_SOURCE_ANALYSIS_PROGRESS_CHANNEL_ID,
                context.getString(R.string.vocabulary_source_analysis_progress_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(R.string.vocabulary_source_analysis_progress_channel_description)
                setSound(null, null)
                enableVibration(false)
            }

            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    private fun createVocabularySourceAnalysisSuccessChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                VOCABULARY_SOURCE_ANALYSIS_SUCCESS_CHANNEL_ID,
                context.getString(R.string.vocabulary_source_analysis_success_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.vocabulary_source_analysis_success_channel_description)
                lightColor = Color.GREEN
                enableLights(true)
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    private fun createVocabularySourceAnalysisFailureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                VOCABULARY_SOURCE_ANALYSIS_FAILURE_CHANNEL_ID,
                context.getString(R.string.vocabulary_source_analysis_failure_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.vocabulary_source_analysis_failure_channel_description)
                lightColor = Color.RED
                enableLights(true)
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    private fun createWorkLogicChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                WORK_LOGIC_NOTIFICATION_CHANNEL_ID,
                WORK_LOGIC_NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description =
                    context.getString(R.string.work_logic_notification_channel_description)
                lightColor = Color.GREEN
                enableLights(true)
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    private fun createDeckRepetitionChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                DECK_REPETITION_CHANNEL_ID,
                DECK_REPETITION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.deck_repetition_channel_description)
                lightColor = Color.GREEN
                enableLights(true)
                enableVibration(true)

                val soundUri =
                    Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${R.raw.deck_review_notification_sound}")
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()

                setSound(soundUri, audioAttributes)
            }

            notificationManager.createNotificationChannel(notificationChannel)
        }
    }
}
