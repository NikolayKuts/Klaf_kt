package com.example.klaf.presentation.auxiliary

import androidx.lifecycle.*
import kotlinx.coroutines.*

private const val TIME_FORMAT_TEMPLATE = "%02d:%02d"
private const val DELAY_INTERVAL: Long = 1000
private const val SECOND_QUANTITY_IN_MINUTE = 60

class RepeatTimer : DefaultLifecycleObserver, ViewModel() {

    var isRunning = false
    var onAction: () -> Unit = {}
    val time: LiveData<String> get() = _time
    val savedTotalTime get() = _savedTotalTime

    private val isNotRunning get() = !isRunning
    private var isPaused = false
    private val _time = MutableLiveData<String>()
    private var totalSeconds: Long = 0
    private var _savedTotalTime: Long = 0
    private val scope = CoroutineScope(Dispatchers.IO)
    private var job: Job? = null

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        if (isPaused) {
            runCounting()
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        pauseCounting()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        scope.cancel()
    }

    fun runCounting() {
        if (isNotRunning) {
            isRunning = true
            isPaused = false
            onAction()
            job = scope.launch {
                while (isRunning) {
                    delay(DELAY_INTERVAL)
                    totalSeconds++
                    _time.postValue(getTimeAsString())
                }
            }
        }
    }

    fun stopCounting() {
        job?.cancel()
        isRunning = false
        isPaused = false
        _savedTotalTime = totalSeconds
        totalSeconds = 0
        onAction()
        _time.postValue(getTimeAsString())
    }

    private fun pauseCounting() {
        job?.cancel()
        isRunning = false
        isPaused = true
        onAction()
    }

    private fun getTimeAsString(): String {
        val seconds = totalSeconds % SECOND_QUANTITY_IN_MINUTE
        val minutes = totalSeconds / SECOND_QUANTITY_IN_MINUTE
        return TIME_FORMAT_TEMPLATE.format(minutes, seconds)
    }
}
