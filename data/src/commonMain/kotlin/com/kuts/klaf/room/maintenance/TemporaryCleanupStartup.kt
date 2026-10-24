package com.kuts.klaf.room.maintenance

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CancellationException

sealed interface TemporaryCleanupStartupState {
    data object Running : TemporaryCleanupStartupState
    data object Ready : TemporaryCleanupStartupState
    data class Report(val result: TemporaryCleanupResult) : TemporaryCleanupStartupState
    data class Failed(val message: String) : TemporaryCleanupStartupState
}

// TEMPORARY: keeps the normal UI closed until cleanup and its receipt succeed.
class TemporaryCleanupStartup(private val cleanup: suspend () -> TemporaryCleanupResult) {
    private val mutableState = MutableStateFlow<TemporaryCleanupStartupState>(TemporaryCleanupStartupState.Running)
    val state: StateFlow<TemporaryCleanupStartupState> = mutableState.asStateFlow()

    suspend fun run() {
        mutableState.value = TemporaryCleanupStartupState.Running
        try {
            val result = cleanup()
            mutableState.value = if (result is TemporaryCleanupResult.Completed && result.alreadyCompleted) {
                TemporaryCleanupStartupState.Ready
            } else TemporaryCleanupStartupState.Report(result)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            mutableState.value = TemporaryCleanupStartupState.Failed(failure.message ?: "Cleanup failed")
        }
    }

    fun openApp() {
        val report = mutableState.value as? TemporaryCleanupStartupState.Report ?: return
        if (report.result is TemporaryCleanupResult.Completed) mutableState.value = TemporaryCleanupStartupState.Ready
    }
}
