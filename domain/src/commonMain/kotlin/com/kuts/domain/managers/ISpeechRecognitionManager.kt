package com.kuts.domain.managers

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Platform speech-to-text. Only one session may run at a time: [startRecognition] is ignored
 * while [state] is active, so callers never have to serialize the requests themselves.
 *
 * The manager keeps no knowledge of what the text is dictated into. [targetId] is an opaque
 * caller-owned tag that is echoed back in [state] and in [recognizedText], which lets a caller
 * with several input fields route the result without tracking the active field itself.
 */
interface ISpeechRecognitionManager {

    /** `false` where the platform has no implementation yet: every call is then a no-op. */
    val isSupported: Boolean

    val state: StateFlow<SpeechRecognitionState>

    /** Final results only. Partial ones are carried by [SpeechRecognitionState.Listening]. */
    val recognizedText: SharedFlow<SpeechRecognitionResult>

    fun startRecognition(targetId: String)

    /** Stops listening and still delivers what was recognized so far. */
    fun stopRecognition()

    /** Stops listening and drops the result. */
    fun cancelRecognition()

    fun release()
}

sealed interface SpeechRecognitionState {

    val targetId: String?

    data object Idle : SpeechRecognitionState {

        override val targetId: String? = null
    }

    data class Starting(override val targetId: String) : SpeechRecognitionState

    data class Listening(
        override val targetId: String,
        val partialText: String = "",
    ) : SpeechRecognitionState

    data class Processing(override val targetId: String) : SpeechRecognitionState

    data class Error(
        override val targetId: String?,
        val reason: SpeechRecognitionError,
    ) : SpeechRecognitionState

    data object Unsupported : SpeechRecognitionState {

        override val targetId: String? = null
    }
}

enum class SpeechRecognitionError {
    PERMISSION_DENIED,
    NO_SPEECH_DETECTED,
    NETWORK,
    BUSY,
    UNSUPPORTED,
    UNKNOWN,
}

data class SpeechRecognitionResult(
    val targetId: String,
    val text: String,
)

val SpeechRecognitionState.isActive: Boolean
    get() = when (this) {
        is SpeechRecognitionState.Starting,
        is SpeechRecognitionState.Listening,
        is SpeechRecognitionState.Processing,
        -> true

        SpeechRecognitionState.Idle,
        SpeechRecognitionState.Unsupported,
        is SpeechRecognitionState.Error,
        -> false
    }
