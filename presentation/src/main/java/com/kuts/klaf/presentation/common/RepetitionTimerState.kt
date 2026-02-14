package com.kuts.klaf.presentation.common

data class RepetitionTimerState(
    val time: String,
    val totalSeconds: Long,
    val countingState: TimerCountingState,
)