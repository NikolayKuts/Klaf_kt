package com.kuts.klaf.presentation.common

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.kuts.klaf.presentation.common.TimerCountingState.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

class RepetitionTimer @Inject constructor() : DefaultLifecycleObserver {

    companion object {

        private const val DELAY_INTERVAL: Long = 1000
        private const val INITIAL_TIME_VALUE: Long = 0
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private var job: Job? = null

    private var totalSeconds: Long = 0

    var savedTotalTimeInSeconds: Long = 0
        private set

    private val time = MutableStateFlow(value = INITIAL_TIME_VALUE.timeAsString)
    private val timerCountingState = MutableStateFlow(STOPPED)

    val timerState = combine(time, timerCountingState) { time, countingState ->
        RepetitionTimerState(time = time, countingState = countingState)
    }.stateIn(
        scope = scope,
        started = SharingStarted.Lazily,
        initialValue = RepetitionTimerState(time.value, countingState = timerCountingState.value)
    )

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        if (timerCountingState.value == PAUSED) {
            runCounting()
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        if (timerCountingState.value == RUN) {
            job?.cancel()
            timerCountingState.value = PAUSED
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        if (timerCountingState.value == RUN) {
            job?.cancel()
            timerCountingState.value = PAUSED
        }
    }

    fun runCounting() {
        if (timerCountingState.value != RUN) {
            timerCountingState.value = RUN

            job = scope.launch {
                while (timerCountingState.value == RUN) {
                    delay(DELAY_INTERVAL)
                    totalSeconds++
                    time.value = totalSeconds.timeAsString
                }
            }
        }
    }

    fun stopCounting() {
        job?.cancel()
        timerCountingState.value = STOPPED
        savedTotalTimeInSeconds = totalSeconds
        totalSeconds = 0
        time.value = totalSeconds.timeAsString
    }

    fun resumeCounting() {
        if (timerCountingState.value == PAUSED || timerCountingState.value == FORCIBLY_PAUSED) {
            runCounting()
        }
    }

    fun pauseCounting() {
        if (timerCountingState.value == RUN) {
            job?.cancel()
            timerCountingState.value = FORCIBLY_PAUSED
        }
    }

    fun disableAndClear() {
        scope.cancel()
        timerCountingState.value = STOPPED
    }
}