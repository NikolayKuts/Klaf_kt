package com.kuts.klaf.cardManagement.common

import com.kuts.domain.managers.ISpeechRecognitionManager
import com.kuts.domain.managers.SpeechRecognitionError
import com.kuts.domain.managers.SpeechRecognitionState
import com.kuts.klaf.common.permissions.IMicrophonePermissionManager
import com.kuts.klaf.common.permissions.MicrophonePermissionRequestResult
import com.lib.lokdroid.core.logE
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Drives voice dictation for the mnemonic comment fields: asks for the microphone permission,
 * starts a single recognition session at a time and translates the platform recognition state
 * into what the mnemonic management screen shows.
 *
 * It holds no platform API of its own, which keeps the view model free of them as well.
 */
internal class MnemonicSpeechInputCoordinator(
    private val speechRecognitionManager: ISpeechRecognitionManager,
    private val microphonePermissionManager: IMicrophonePermissionManager,
    private val scope: CoroutineScope,
) {

    private val _state = MutableStateFlow(
        value = MnemonicSpeechInputUiState(isAvailable = speechRecognitionManager.isSupported),
    )
    val state = _state.asStateFlow()

    private val _recognizedText = MutableSharedFlow<MnemonicRecognizedText>(extraBufferCapacity = 1)
    val recognizedText = _recognizedText.asSharedFlow()

    private val _failures = MutableSharedFlow<MnemonicSpeechInputFailure>(extraBufferCapacity = 1)
    val failures = _failures.asSharedFlow()

    private var permissionJob: Job? = null

    /**
     * The permission request happens before the recognition session exists, so the recognition
     * state cannot describe that step. While this is `true` the platform state is ignored and the
     * optimistic [MnemonicSpeechInputUiState.Phase.STARTING] stays on the screen.
     */
    private var isAwaitingPermission = false

    init {
        observeRecognitionState()
        observeRecognizedText()
    }

    fun start(field: MnemonicCommentField) {
        if (!speechRecognitionManager.isSupported) {
            _failures.tryEmit(value = MnemonicSpeechInputFailure.UNSUPPORTED)
            return
        }

        if (_state.value.isBusy) return

        isAwaitingPermission = true
        _state.update { state ->
            state.copy(
                activeField = field,
                phase = MnemonicSpeechInputUiState.Phase.STARTING,
            )
        }

        permissionJob = scope.launch {
            // The permission controller is bound to an activity and throws once that binding is
            // gone -- a rotation during the system dialog is enough. Left unhandled it would take
            // the scope down with it and, worse, strand every microphone button on this screen:
            // the busy flag is only ever cleared by reaching one of the branches below.
            val result = try {
                microphonePermissionManager.requestPermissionIfNeeded()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                logE("Microphone permission request failed\n${throwable.stackTraceToString()}")
                isAwaitingPermission = false
                resetSession()
                _failures.tryEmit(value = MnemonicSpeechInputFailure.UNKNOWN)
                return@launch
            }

            isAwaitingPermission = false

            when (result) {
                MicrophonePermissionRequestResult.GRANTED -> {
                    speechRecognitionManager.startRecognition(targetId = field.name)
                }

                MicrophonePermissionRequestResult.DENIED -> {
                    resetSession()
                    _failures.tryEmit(value = MnemonicSpeechInputFailure.PERMISSION_DENIED)
                }

                MicrophonePermissionRequestResult.DENIED_ALWAYS -> {
                    resetSession()
                    _failures.tryEmit(value = MnemonicSpeechInputFailure.PERMISSION_DENIED_ALWAYS)
                }
            }
        }
    }

    /** Ends listening and drops the result. */
    fun cancel() {
        permissionJob?.cancel()
        isAwaitingPermission = false
        resetSession()
        speechRecognitionManager.cancelRecognition()
    }

    fun release() {
        permissionJob?.cancel()
        isAwaitingPermission = false
        speechRecognitionManager.release()
    }

    private fun observeRecognitionState() {
        speechRecognitionManager.state
            .onEach { recognitionState -> handleRecognitionState(recognitionState = recognitionState) }
            .launchIn(scope)
    }

    private fun observeRecognizedText() {
        speechRecognitionManager.recognizedText
            .onEach { result ->
                val field = result.targetId.toCommentField() ?: return@onEach

                _recognizedText.tryEmit(
                    value = MnemonicRecognizedText(field = field, text = result.text),
                )
            }
            .launchIn(scope)
    }

    private fun handleRecognitionState(recognitionState: SpeechRecognitionState) {
        if (isAwaitingPermission) return

        when (recognitionState) {
            is SpeechRecognitionState.Starting -> {
                updatePhase(
                    targetId = recognitionState.targetId,
                    phase = MnemonicSpeechInputUiState.Phase.STARTING,
                )
            }

            is SpeechRecognitionState.Listening -> {
                updatePhase(
                    targetId = recognitionState.targetId,
                    phase = MnemonicSpeechInputUiState.Phase.LISTENING,
                )
            }

            is SpeechRecognitionState.Processing -> {
                updatePhase(
                    targetId = recognitionState.targetId,
                    phase = MnemonicSpeechInputUiState.Phase.PROCESSING,
                )
            }

            SpeechRecognitionState.Idle -> resetSession()

            is SpeechRecognitionState.Error -> {
                resetSession()
                _failures.tryEmit(value = recognitionState.reason.toFailure())
            }

            SpeechRecognitionState.Unsupported -> {
                resetSession()
                _state.update { state -> state.copy(isAvailable = false) }
                _failures.tryEmit(value = MnemonicSpeechInputFailure.UNSUPPORTED)
            }
        }
    }

    private fun updatePhase(targetId: String, phase: MnemonicSpeechInputUiState.Phase) {
        val field = targetId.toCommentField() ?: return

        _state.update { state -> state.copy(activeField = field, phase = phase) }
    }

    private fun resetSession() {
        _state.update { state ->
            state.copy(activeField = null, phase = MnemonicSpeechInputUiState.Phase.IDLE)
        }
    }

    private fun String.toCommentField(): MnemonicCommentField? {
        return MnemonicCommentField.entries.firstOrNull { field -> field.name == this }
    }

    private fun SpeechRecognitionError.toFailure(): MnemonicSpeechInputFailure = when (this) {
        SpeechRecognitionError.PERMISSION_DENIED -> MnemonicSpeechInputFailure.PERMISSION_DENIED
        SpeechRecognitionError.NO_SPEECH_DETECTED -> MnemonicSpeechInputFailure.NO_SPEECH_DETECTED
        SpeechRecognitionError.NETWORK -> MnemonicSpeechInputFailure.NETWORK
        SpeechRecognitionError.BUSY -> MnemonicSpeechInputFailure.BUSY
        SpeechRecognitionError.UNSUPPORTED -> MnemonicSpeechInputFailure.UNSUPPORTED
        SpeechRecognitionError.UNKNOWN -> MnemonicSpeechInputFailure.UNKNOWN
    }
}
