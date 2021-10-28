package com.example.klaf.presentation.auxiliary

import android.content.Context
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.preference.PreferenceManager
import com.example.klaf.R
import kotlinx.coroutines.*

private const val TIME_FORMAT_TEMPLATE = "%02d:%02d"
private const val DELAY_INTERVAL: Long = 1000
private const val SECOND_QUANTITY_IN_MINUTE = 60
private const val TIMER_START_POSITION = "00:00"
private const val SAVED_TIME_KEY = "saved_time_key"
private const val TIMER_RUNNING_STATE_KEY = "timer_state_key"
private const val TIMER_PAUSING_STATE_KEY = "timer_pausing_state_key"

class RepeatTimer(private val timerTextView: TextView, private val context: Context) :
    DefaultLifecycleObserver {

    var isRunning = false
    var isPaused = false
    private val isNotRunning get() = !isRunning

    private var totalSeconds: Long = 0

    fun runCounting() {
        if (isNotRunning) {
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
    }

    fun stopCounting() {
        isRunning = false
        setColorByTimerState()
        totalSeconds = 0
        timerTextView.text = TIMER_START_POSITION
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)

        when {
            preferences.getBoolean(TIMER_RUNNING_STATE_KEY, false) -> {
                totalSeconds = preferences.getLong(SAVED_TIME_KEY, 0)
                runCounting()
            }
            preferences.getBoolean(TIMER_PAUSING_STATE_KEY, false) -> {
                totalSeconds = preferences.getLong(SAVED_TIME_KEY, 0)
                pauseCounting()
            }
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        if (isRunning || isPaused) {
            saveTimerStateInSharedPreferences(totalSeconds)
            pauseCounting()
        } else {
            saveTimerStateInSharedPreferences(0)
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        saveTimerStateInSharedPreferences(0)
    }

    private fun pauseCounting() {
        isRunning = false
        isPaused = true
        setColorByTimerState()
    }

    private fun getTimeAsString(): String {
        val seconds = totalSeconds % SECOND_QUANTITY_IN_MINUTE
        val minutes = totalSeconds / SECOND_QUANTITY_IN_MINUTE
        return TIME_FORMAT_TEMPLATE.format(minutes, seconds)
    }

    private fun setColorByTimerState() {
        val context = timerTextView.context
        if (isRunning) {
            timerTextView.setTextColor(ContextCompat.getColor(context, R.color.timer_is_running))
        } else {
            timerTextView.setTextColor(
                ContextCompat.getColor(context, R.color.timer_is_not_running)
            )
        }
    }

    private fun saveTimerStateInSharedPreferences(
        totalSeconds: Long,
        isRunning: Boolean = this.isRunning,
        isPaused: Boolean = this.isPaused,
    ) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        preferences.edit().putLong(SAVED_TIME_KEY, totalSeconds).apply()
        preferences.edit().putBoolean(TIMER_RUNNING_STATE_KEY, isRunning).apply()
        preferences.edit().putBoolean(TIMER_PAUSING_STATE_KEY, isPaused).apply()
    }
}
