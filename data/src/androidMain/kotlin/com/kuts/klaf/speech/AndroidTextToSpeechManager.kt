package com.kuts.klaf.speech

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import com.kuts.domain.managers.ITextToSpeechManager
import com.lib.lokdroid.core.logD
import com.lib.lokdroid.core.logE
import java.util.Locale

private val PreferredVoiceNames = listOf(
    "en-us-x-iom-network",
    "en-us-x-iol-network",
    "en-us-x-tpc-network",
)

class AndroidTextToSpeechManager(
    context: Context,
) : ITextToSpeechManager {

    private var isInitialized = false
    private var isShutdown = false
    private var pendingText: String? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val textToSpeech = TextToSpeech(context.applicationContext) { status ->
        mainHandler.post { if (!isShutdown) handleInitStatus(status = status) }
    }

    override fun speak(text: String) {
        val trimmedText = text.trim()
        if (trimmedText.isBlank()) return

        pendingText = trimmedText
        speakPendingText()
    }

    override fun stop() {
        pendingText = null
        textToSpeech.stop()
    }

    override fun shutdown() {
        isShutdown = true
        pendingText = null
        textToSpeech.stop()
        textToSpeech.shutdown()
    }

    private fun handleInitStatus(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            logE("Android TextToSpeech initialization failed: status=$status")
            return
        }

        val languageResult = textToSpeech.setLanguage(Locale.US)
        if (languageResult == TextToSpeech.LANG_MISSING_DATA ||
            languageResult == TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            logE("Android TextToSpeech US English is not supported: result=$languageResult")
            return
        }

        val preferredVoice = PreferredVoiceNames
            .mapNotNull { voiceName -> textToSpeech.voices.orEmpty().firstOrNull { voice -> voice.name == voiceName } }
            .firstOrNull()

        if (preferredVoice == null) {
            logE("Android TextToSpeech preferred US English voice is unavailable.")
        } else {
            textToSpeech.voice = preferredVoice
            logD("Android TextToSpeech voice selected: ${preferredVoice.name}")
        }

        isInitialized = true
        speakPendingText()
    }

    private fun speakPendingText() {
        if (!isInitialized || isShutdown) return

        val text = pendingText ?: return
        pendingText = null
        textToSpeech.stop()
        textToSpeech.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            null,
        )
    }
}
