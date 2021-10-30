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

    private val isNotRunning get() = !isRunning
    private var isPaused = false
    private val _time = MutableLiveData<String>()
    private var totalSeconds: Long = 0

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

    fun runCounting() {
        if (isNotRunning) {
            isRunning = true
            isPaused = false
            onAction()
            CoroutineScope(Dispatchers.IO).launch {
                while (isRunning) {
                    delay(DELAY_INTERVAL)
                    totalSeconds++
                    _time.postValue(getTimeAsString())
                }
            }
        }
    }

    fun stopCounting() {
        isRunning = false
        isPaused = false
        totalSeconds = 0
        onAction()
    }

    private fun pauseCounting() {
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
