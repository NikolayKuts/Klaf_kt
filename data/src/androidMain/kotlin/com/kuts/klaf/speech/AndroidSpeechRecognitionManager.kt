package com.kuts.klaf.speech

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import com.kuts.domain.managers.ISpeechRecognitionManager
import com.kuts.domain.managers.SpeechRecognitionError
import com.kuts.domain.managers.SpeechRecognitionResult
import com.kuts.domain.managers.SpeechRecognitionState
import com.kuts.domain.managers.isActive
import com.lib.lokdroid.core.logD
import com.lib.lokdroid.core.logE
import java.util.Locale
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Speech-to-text on top of the platform [SpeechRecognizer]. No ML Kit and no third-party engine:
 * the recognition service the device already ships with does the work.
 *
 * [SpeechRecognizer] is a main-thread-only API, so every call is posted to the main looper and the
 * recognizer instance is created lazily there.
 */
class AndroidSpeechRecognitionManager(
    context: Context,
) : ISpeechRecognitionManager {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _state = MutableStateFlow<SpeechRecognitionState>(
        value = SpeechRecognitionState.Idle,
    )
    override val state = _state.asStateFlow()

    private val _recognizedText = MutableSharedFlow<SpeechRecognitionResult>(
        extraBufferCapacity = 1,
    )
    override val recognizedText = _recognizedText.asSharedFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var activeTargetId: String? = null

    override val isSupported: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(appContext)

    override fun startRecognition(targetId: String) {
        if (!isSupported) {
            _state.value = SpeechRecognitionState.Unsupported
            return
        }

        if (_state.value.isActive) {
            logD("Speech recognition is already running, request is ignored: $targetId")
            return
        }

        // Starting is published before any early return so that a terminal state always differs
        // from the previous one: two identical states in a row would not reach the StateFlow
        // collectors and would leave the caller waiting for a session that already ended.
        activeTargetId = targetId
        _state.value = SpeechRecognitionState.Starting(targetId = targetId)

        if (!isRecordAudioPermissionGranted()) {
            onSessionFinished(
                finalState = SpeechRecognitionState.Error(
                    targetId = targetId,
                    reason = SpeechRecognitionError.PERMISSION_DENIED,
                ),
            )
            return
        }

        runOnMainThread {
            val recognizer = obtainRecognizer()

            if (recognizer == null) {
                onSessionFinished(
                    finalState = SpeechRecognitionState.Error(
                        targetId = targetId,
                        reason = SpeechRecognitionError.UNSUPPORTED,
                    ),
                )
                return@runOnMainThread
            }

            runCatching { recognizer.startListening(createRecognitionIntent()) }
                .onFailure { throwable ->
                    logE("Failed to start speech recognition\n${throwable.stackTraceToString()}")
                    onSessionFinished(
                        finalState = SpeechRecognitionState.Error(
                            targetId = targetId,
                            reason = SpeechRecognitionError.UNKNOWN,
                        ),
                    )
                }
        }
    }

    override fun stopRecognition() {
        if (!_state.value.isActive) return

        runOnMainThread { speechRecognizer?.stopListening() }
    }

    override fun cancelRecognition() {
        onSessionFinished(finalState = SpeechRecognitionState.Idle)

        runOnMainThread { speechRecognizer?.cancel() }
    }

    override fun release() {
        onSessionFinished(finalState = SpeechRecognitionState.Idle)

        runOnMainThread {
            speechRecognizer?.destroy()
            speechRecognizer = null
        }
    }

    private fun isRecordAudioPermissionGranted(): Boolean {
        val status = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.RECORD_AUDIO,
        )

        return status == PackageManager.PERMISSION_GRANTED
    }

    private fun obtainRecognizer(): SpeechRecognizer? {
        speechRecognizer?.let { recognizer -> return recognizer }

        return runCatching {
            SpeechRecognizer.createSpeechRecognizer(appContext).apply {
                setRecognitionListener(recognitionListener)
            }
        }.onFailure { throwable ->
            logE("Failed to create SpeechRecognizer\n${throwable.stackTraceToString()}")
        }.getOrNull()?.also { recognizer -> speechRecognizer = recognizer }
    }

    private fun createRecognitionIntent(): Intent {
        // Without EXTRA_LANGUAGE the recognizer falls back to the language configured in the
        // device's own voice-search settings, not to the phone's locale: speaking Russian into a
        // recognizer left on English produces English-looking nonsense.
        val languageTag = currentLanguageTag()

        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
    }

    private fun currentLanguageTag(): String {
        val locales = appContext.resources.configuration.locales
        val locale = locales.takeIf { it.size() > 0 }?.get(0) ?: Locale.getDefault()

//        return locale.toLanguageTag()
        return "ru-RU"
    }

    private fun runOnMainThread(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            mainHandler.post(action)
        }
    }

    private fun onSessionFinished(finalState: SpeechRecognitionState) {
        activeTargetId = null
        _state.value = finalState
    }

    private val recognitionListener = object : RecognitionListener {

        override fun onReadyForSpeech(params: Bundle?) {
            val targetId = activeTargetId ?: return

            _state.value = SpeechRecognitionState.Listening(targetId = targetId)
        }

        override fun onBeginningOfSpeech() = Unit

        override fun onRmsChanged(rmsdB: Float) = Unit

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() {
            val targetId = activeTargetId ?: return

            _state.value = SpeechRecognitionState.Processing(targetId = targetId)
        }

        override fun onError(error: Int) {
            logE("Speech recognition error: $error")

            // A session that already ended still gets callbacks: cancel() is answered with
            // ERROR_CLIENT, and a delivered result is sometimes followed by ERROR_NO_MATCH.
            // Reporting those would replace a finished -- often successful -- session with a
            // failure message about a session nobody is waiting for any more.
            val targetId = activeTargetId ?: return

            onSessionFinished(
                finalState = SpeechRecognitionState.Error(
                    targetId = targetId,
                    reason = error.toRecognitionError(),
                ),
            )
        }

        override fun onResults(results: Bundle?) {
            val targetId = activeTargetId ?: return
            val text = results.firstRecognizedText()

            if (text == null) {
                onSessionFinished(
                    finalState = SpeechRecognitionState.Error(
                        targetId = targetId,
                        reason = SpeechRecognitionError.NO_SPEECH_DETECTED,
                    ),
                )
                return
            }

            _recognizedText.tryEmit(
                value = SpeechRecognitionResult(targetId = targetId, text = text),
            )
            onSessionFinished(finalState = SpeechRecognitionState.Idle)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val targetId = activeTargetId ?: return
            val partialText = partialResults.firstRecognizedText() ?: return

            _state.value = SpeechRecognitionState.Listening(
                targetId = targetId,
                partialText = partialText,
            )
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private fun Bundle?.firstRecognizedText(): String? {
        return this?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            ?.takeIf(String::isNotBlank)
    }

    private fun Int.toRecognitionError(): SpeechRecognitionError = when (this) {
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> SpeechRecognitionError.PERMISSION_DENIED

        SpeechRecognizer.ERROR_NO_MATCH,
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
        -> SpeechRecognitionError.NO_SPEECH_DETECTED

        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
        SpeechRecognizer.ERROR_SERVER,
        -> SpeechRecognitionError.NETWORK

        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> SpeechRecognitionError.BUSY

        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE,
        -> SpeechRecognitionError.UNSUPPORTED

        else -> SpeechRecognitionError.UNKNOWN
    }
}
