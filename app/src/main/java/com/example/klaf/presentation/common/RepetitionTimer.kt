package com.example.klaf.presentation.common

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.klaf.presentation.common.TimerCountingState.*
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

    var savedTotalTime: Long = 0
        private set
    val savedTotalTimeAsString get() = savedTotalTime.timeAsString

    private val _time = MutableStateFlow(value = INITIAL_TIME_VALUE.timeAsString)
    private val timerCountingState = MutableStateFlow(STOPED)

    val timerState = combine(_time, timerCountingState) { time, countingState ->
        RepetitionTimerState(time = time, countingState = countingState)
    }.stateIn(
        scope = scope,
        started = SharingStarted.Lazily,
        initialValue = RepetitionTimerState(_time.value, countingState = timerCountingState.value)
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
            pauseCounting()
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        scope.cancel()
        timerCountingState.value = STOPED
    }

    fun runCounting() {
        if (timerCountingState.value != RUN) {
            timerCountingState.value = RUN

            job = scope.launch {
                while (timerCountingState.value == RUN) {
                    delay(DELAY_INTERVAL)
                    totalSeconds++
                    _time.value = totalSeconds.timeAsString
                }
            }
        }
    }

    fun stopCounting() {
        job?.cancel()
        timerCountingState.value = STOPED
        savedTotalTime = totalSeconds
        totalSeconds = 0
        _time.value = totalSeconds.timeAsString
    }

    fun resumeCounting() {
        if (timerCountingState.value == PAUSED) {
            runCounting()
        }
    }

    fun pauseCounting() {
        if (timerCountingState.value == RUN) {
            job?.cancel()
            timerCountingState.value = PAUSED
        }
    }
}
