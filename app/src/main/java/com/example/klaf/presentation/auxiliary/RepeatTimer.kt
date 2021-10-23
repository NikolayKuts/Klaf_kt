package com.example.klaf.presentation.auxiliary

import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.klaf.R
import kotlinx.coroutines.*

private const val TIME_FORMAT_TEMPLATE = "%02d:%02d"
private const val DELAY_INTERVAL: Long = 1000
private const val SECOND_QUANTITY_IN_MINUTE = 60
private const val TIMER_START_POSITION = "00:00"

class RepeatTimer(private val timerTextView: TextView) {

    var isRunning = false
    var isPaused = false
    var savedTotalSeconds = 0
    private var totalSeconds = 0

    fun runCounting() {
        isRunning = true
        isPaused = false
        setColorByTimerState()
        CoroutineScope(Dispatchers.IO).launch {
            while (isRunning) {
                delay(DELAY_INTERVAL)
                totalSeconds++
                withContext(Dispatchers.Main) { timerTextView.text = getTimeAsString() }
            }
        }
    }

    fun stopCounting() {
        isRunning = false
        setColorByTimerState()
        savedTotalSeconds = totalSeconds
        totalSeconds = 0
        timerTextView.text = TIMER_START_POSITION
    }

    fun pauseCounting() {
        isRunning = false
        isPaused = true
        setColorByTimerState()
    }

    private fun getTimeAsString(): String {
        val seconds = totalSeconds % SECOND_QUANTITY_IN_MINUTE
        val minutes = totalSeconds / SECOND_QUANTITY_IN_MINUTE
        return TIME_FORMAT_TEMPLATE.format(minutes, seconds)
    }

    fun setColorByTimerState() {
        val context = timerTextView.context
        if (isRunning) {
            timerTextView.setTextColor(ContextCompat.getColor(context, R.color.timer_is_running))
        } else {
            timerTextView.setTextColor(
                ContextCompat.getColor(context, R.color.timer_is_not_running)
            )
        }
    }

}
