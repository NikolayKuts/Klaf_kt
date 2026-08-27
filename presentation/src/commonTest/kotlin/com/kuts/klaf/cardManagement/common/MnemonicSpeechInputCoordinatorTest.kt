package com.kuts.klaf.cardManagement.common

import com.kuts.domain.managers.ISpeechRecognitionManager
import com.kuts.domain.managers.SpeechRecognitionError
import com.kuts.domain.managers.SpeechRecognitionResult
import com.kuts.domain.managers.SpeechRecognitionState
import com.kuts.domain.managers.isActive
import com.kuts.klaf.common.permissions.IMicrophonePermissionManager
import com.kuts.klaf.common.permissions.MicrophonePermissionRequestResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MnemonicSpeechInputCoordinatorTest {

    @Test
    fun `a second session is refused while the first one is running`() = runTest {
        val speechRecognitionManager = FakeSpeechRecognitionManager()
        val coordinator = createCoordinator(
            speechRecognitionManager = speechRecognitionManager,
            scope = backgroundScope,
        )

        coordinator.start(field = MnemonicCommentField.ASSOCIATION)
        runCurrent()
        speechRecognitionManager.emitListening(targetId = MnemonicCommentField.ASSOCIATION.name)
        runCurrent()

        coordinator.start(field = MnemonicCommentField.IMAGE)
        runCurrent()

        assertEquals(
            expected = listOf(MnemonicCommentField.ASSOCIATION.name),
            actual = speechRecognitionManager.startedTargetIds,
        )
        assertEquals(
            expected = MnemonicCommentField.ASSOCIATION,
            actual = coordinator.state.value.activeField,
        )
    }

    @Test
    fun `a new session can start once the previous one has finished`() = runTest {
        val speechRecognitionManager = FakeSpeechRecognitionManager()
        val coordinator = createCoordinator(
            speechRecognitionManager = speechRecognitionManager,
            scope = backgroundScope,
        )

        coordinator.start(field = MnemonicCommentField.ASSOCIATION)
        runCurrent()
        speechRecognitionManager.emitResult(
            targetId = MnemonicCommentField.ASSOCIATION.name,
            text = "make it funny",
        )
        runCurrent()

        coordinator.start(field = MnemonicCommentField.IMAGE)
        runCurrent()

        assertEquals(
            expected = listOf(
                MnemonicCommentField.ASSOCIATION.name,
                MnemonicCommentField.IMAGE.name,
            ),
            actual = speechRecognitionManager.startedTargetIds,
        )
    }

    @Test
    fun `recognized text is reported for the association comment`() = runTest {
        val speechRecognitionManager = FakeSpeechRecognitionManager()
        val coordinator = createCoordinator(
            speechRecognitionManager = speechRecognitionManager,
            scope = backgroundScope,
        )
        val recognized = mutableListOf<MnemonicRecognizedText>()

        backgroundScope.launch { coordinator.recognizedText.toList(destination = recognized) }
        runCurrent()

        coordinator.start(field = MnemonicCommentField.ASSOCIATION)
        runCurrent()
        speechRecognitionManager.emitResult(
            targetId = MnemonicCommentField.ASSOCIATION.name,
            text = "make it funny",
        )
        runCurrent()

        assertEquals(
            expected = listOf(
                MnemonicRecognizedText(
                    field = MnemonicCommentField.ASSOCIATION,
                    text = "make it funny",
                ),
            ),
            actual = recognized,
        )
        assertNull(actual = coordinator.state.value.activeField)
    }

    @Test
    fun `recognized text is reported for the image comment`() = runTest {
        val speechRecognitionManager = FakeSpeechRecognitionManager()
        val coordinator = createCoordinator(
            speechRecognitionManager = speechRecognitionManager,
            scope = backgroundScope,
        )
        val recognized = mutableListOf<MnemonicRecognizedText>()

        backgroundScope.launch { coordinator.recognizedText.toList(destination = recognized) }
        runCurrent()

        coordinator.start(field = MnemonicCommentField.IMAGE)
        runCurrent()
        speechRecognitionManager.emitResult(
            targetId = MnemonicCommentField.IMAGE.name,
            text = "bright colors",
        )
        runCurrent()

        assertEquals(
            expected = listOf(
                MnemonicRecognizedText(
                    field = MnemonicCommentField.IMAGE,
                    text = "bright colors",
                ),
            ),
            actual = recognized,
        )
    }

    @Test
    fun `a denied microphone permission reports a failure and starts nothing`() = runTest {
        val speechRecognitionManager = FakeSpeechRecognitionManager()
        val coordinator = createCoordinator(
            speechRecognitionManager = speechRecognitionManager,
            microphonePermissionManager = FakeMicrophonePermissionManager(
                result = MicrophonePermissionRequestResult.DENIED,
            ),
            scope = backgroundScope,
        )
        val failures = mutableListOf<MnemonicSpeechInputFailure>()

        backgroundScope.launch { coordinator.failures.toList(destination = failures) }
        runCurrent()

        coordinator.start(field = MnemonicCommentField.ASSOCIATION)
        runCurrent()

        assertEquals(
            expected = listOf(MnemonicSpeechInputFailure.PERMISSION_DENIED),
            actual = failures,
        )
        assertTrue(actual = speechRecognitionManager.startedTargetIds.isEmpty())
        assertEquals(expected = false, actual = coordinator.state.value.isBusy)
    }

    @Test
    fun `a throwing permission request frees the session instead of stranding it`() = runTest {
        val speechRecognitionManager = FakeSpeechRecognitionManager()
        val coordinator = createCoordinator(
            speechRecognitionManager = speechRecognitionManager,
            microphonePermissionManager = FakeMicrophonePermissionManager(
                failure = IllegalStateException("activity is null, `bind` was never called"),
            ),
            scope = backgroundScope,
        )
        val failures = mutableListOf<MnemonicSpeechInputFailure>()

        backgroundScope.launch { coordinator.failures.toList(destination = failures) }
        runCurrent()

        coordinator.start(field = MnemonicCommentField.ASSOCIATION)
        runCurrent()

        assertEquals(expected = listOf(MnemonicSpeechInputFailure.UNKNOWN), actual = failures)
        assertEquals(expected = false, actual = coordinator.state.value.isBusy)
        assertTrue(actual = speechRecognitionManager.startedTargetIds.isEmpty())

        // The whole point of the fix: the next tap still works.
        coordinator.start(field = MnemonicCommentField.ASSOCIATION)
        runCurrent()

        assertEquals(expected = 2, actual = failures.size)
    }

    @Test
    fun `a recognition error is reported and frees the session`() = runTest {
        val speechRecognitionManager = FakeSpeechRecognitionManager()
        val coordinator = createCoordinator(
            speechRecognitionManager = speechRecognitionManager,
            scope = backgroundScope,
        )
        val failures = mutableListOf<MnemonicSpeechInputFailure>()

        backgroundScope.launch { coordinator.failures.toList(destination = failures) }
        runCurrent()

        coordinator.start(field = MnemonicCommentField.ASSOCIATION)
        runCurrent()
        speechRecognitionManager.emitError(
            targetId = MnemonicCommentField.ASSOCIATION.name,
            reason = SpeechRecognitionError.NO_SPEECH_DETECTED,
        )
        runCurrent()

        assertEquals(
            expected = listOf(MnemonicSpeechInputFailure.NO_SPEECH_DETECTED),
            actual = failures,
        )
        assertEquals(expected = false, actual = coordinator.state.value.isBusy)
    }

    @Test
    fun `dictation is unavailable when the platform has no recognition`() = runTest {
        val speechRecognitionManager = FakeSpeechRecognitionManager(isSupported = false)
        val coordinator = createCoordinator(
            speechRecognitionManager = speechRecognitionManager,
            scope = backgroundScope,
        )
        val failures = mutableListOf<MnemonicSpeechInputFailure>()

        backgroundScope.launch { coordinator.failures.toList(destination = failures) }
        runCurrent()

        coordinator.start(field = MnemonicCommentField.ASSOCIATION)
        runCurrent()

        assertEquals(expected = false, actual = coordinator.state.value.isAvailable)
        assertEquals(expected = listOf(MnemonicSpeechInputFailure.UNSUPPORTED), actual = failures)
        assertTrue(actual = speechRecognitionManager.startedTargetIds.isEmpty())
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun TestScope.createCoordinator(
    speechRecognitionManager: ISpeechRecognitionManager,
    microphonePermissionManager: IMicrophonePermissionManager = FakeMicrophonePermissionManager(),
    scope: CoroutineScope,
): MnemonicSpeechInputCoordinator {
    val coordinator = MnemonicSpeechInputCoordinator(
        speechRecognitionManager = speechRecognitionManager,
        microphonePermissionManager = microphonePermissionManager,
        scope = scope,
    )

    runCurrent()

    return coordinator
}

private class FakeSpeechRecognitionManager(
    override val isSupported: Boolean = true,
) : ISpeechRecognitionManager {

    private val _state = MutableStateFlow<SpeechRecognitionState>(
        value = SpeechRecognitionState.Idle,
    )
    override val state = _state.asStateFlow()

    private val _recognizedText = MutableSharedFlow<SpeechRecognitionResult>(
        extraBufferCapacity = 1,
    )
    override val recognizedText = _recognizedText.asSharedFlow()

    val startedTargetIds = mutableListOf<String>()

    override fun startRecognition(targetId: String) {
        if (_state.value.isActive) return

        startedTargetIds += targetId
        _state.value = SpeechRecognitionState.Starting(targetId = targetId)
    }

    override fun stopRecognition() = Unit

    override fun cancelRecognition() {
        _state.value = SpeechRecognitionState.Idle
    }

    override fun release() {
        _state.value = SpeechRecognitionState.Idle
    }

    fun emitListening(targetId: String) {
        _state.value = SpeechRecognitionState.Listening(targetId = targetId)
    }

    fun emitResult(targetId: String, text: String) {
        _recognizedText.tryEmit(
            value = SpeechRecognitionResult(targetId = targetId, text = text),
        )
        _state.value = SpeechRecognitionState.Idle
    }

    fun emitError(targetId: String, reason: SpeechRecognitionError) {
        _state.value = SpeechRecognitionState.Error(targetId = targetId, reason = reason)
    }
}

private class FakeMicrophonePermissionManager(
    private val result: MicrophonePermissionRequestResult = MicrophonePermissionRequestResult.GRANTED,
    private val failure: Throwable? = null,
) : IMicrophonePermissionManager {

    override suspend fun requestPermissionIfNeeded(): MicrophonePermissionRequestResult {
        failure?.let { throwable -> throw throwable }

        return result
    }
}
