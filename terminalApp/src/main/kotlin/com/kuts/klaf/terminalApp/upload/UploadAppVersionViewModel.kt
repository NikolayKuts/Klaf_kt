package com.kuts.klaf.terminalApp.upload

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File

class UploadAppVersionViewModel(
    configuration: UploadAppVersionConfiguration,
    private val metadataLoader: UploadGradleMetadataLoader = UploadGradleMetadataLoader(
        projectRoot = File(System.getProperty("user.dir"))
    ),
    private val taskRunner: UploadGradleTaskRunner = UploadGradleTaskRunner(
        projectRoot = File(System.getProperty("user.dir"))
    ),
) {

    var uiState by mutableStateOf(
        UploadAppVersionUiState(
            configuration = configuration,
            selectedAction = UploadAction.Start,
            confirmedAction = null,
            statusKind = UploadStatusKind.Idle,
            statusMessage = DEFAULT_STATUS_MESSAGE,
            statusDetails = null,
            uploadProgress = null,
            isBusy = false,
            isMetadataLoaded = false,
            isCompleted = false,
        )
    )
        private set

    fun focusAction(action: UploadAction) {
        if (uiState.isBusy || uiState.isCompleted) return

        uiState = uiState.copy(
            selectedAction = action,
            confirmedAction = null,
            statusKind = UploadStatusKind.Idle,
            statusMessage = action.focusMessage(),
            statusDetails = null,
            uploadProgress = null,
        )
    }

    suspend fun loadMetadata() {
        uiState = uiState.copy(
            statusKind = UploadStatusKind.Loading,
            statusMessage = "Loading metadata from Gradle...",
            statusDetails = null,
            uploadProgress = null,
            isBusy = true,
        )

        metadataLoader.load(uploadTaskName = resolvedTaskName())
            .onSuccess { configuration ->
                uiState = uiState.copy(
                    configuration = configuration,
                    confirmedAction = null,
                    statusKind = UploadStatusKind.Idle,
                    statusMessage = DEFAULT_STATUS_MESSAGE,
                    statusDetails = null,
                    uploadProgress = null,
                    isBusy = false,
                    isMetadataLoaded = true,
                    isCompleted = false,
                )
            }
            .onFailure { throwable ->
                uiState = uiState.copy(
                    confirmedAction = null,
                    statusKind = UploadStatusKind.Error,
                    statusMessage = "Failed to load Gradle metadata.",
                    statusDetails = throwable.message ?: "unknown error",
                    uploadProgress = null,
                    isBusy = false,
                    isMetadataLoaded = false,
                    isCompleted = false,
                )
            }
    }

    suspend fun confirmSelectedAction(): UploadAppVersionCommand? {
        if (uiState.isBusy) return null

        return when (uiState.selectedAction) {
            UploadAction.Start -> {
                val taskName = resolvedTaskName()
                uiState = uiState.copy(
                    confirmedAction = UploadAction.Start,
                    statusKind = UploadStatusKind.Loading,
                    statusMessage = "Uploading...",
                    statusDetails = "Preparing upload task...",
                    uploadProgress = UploadProgressState(
                        sentBytes = 0L,
                        totalBytes = 0L,
                        percent = 0,
                    ),
                    isBusy = true,
                    isCompleted = false,
                )

                taskRunner.runUploadTask(
                    taskName = taskName,
                    onOutputLine = ::onUploadOutput,
                )
                    .onSuccess {
                        onUploadFinished()
                    }
                    .onFailure { throwable ->
                        onUploadFailed(throwable)
                    }

                null
            }

            UploadAction.Cancel -> {
                uiState = uiState.copy(
                    confirmedAction = UploadAction.Cancel,
                    statusMessage = "Cancel confirmed. Closing the launcher.",
                    isBusy = false,
                )
                UploadAppVersionCommand.Exit
            }
        }
    }

    fun onUploadOutput(
        line: String,
    ) {
        parseUploadProgress(line = line)
            ?.let { progress ->
                uiState = uiState.copy(
                    statusKind = UploadStatusKind.Loading,
                    statusMessage = "Uploading...",
                    statusDetails = "${progress.percent}% uploaded",
                    uploadProgress = progress,
                )
                return
            }

        parseUploadStatus(line = line)
            ?.let { message ->
                uiState = uiState.copy(
                    statusKind = UploadStatusKind.Loading,
                    statusMessage = "Uploading...",
                    statusDetails = message,
                )
                return
            }

        uiState = uiState.copy(
            statusKind = UploadStatusKind.Loading,
            statusMessage = "Uploading...",
            statusDetails = line,
        )
    }

    fun onUploadFinished() {
        uiState = uiState.copy(
            confirmedAction = UploadAction.Start,
            statusKind = UploadStatusKind.Success,
            statusMessage = "Upload task finished successfully.",
            statusDetails = null,
            uploadProgress = uiState.uploadProgress?.copy(percent = 100),
            isBusy = false,
            isMetadataLoaded = true,
            isCompleted = true,
        )
    }

    fun onUploadFailed(
        throwable: Throwable,
    ) {
        uiState = uiState.copy(
            confirmedAction = null,
            statusKind = UploadStatusKind.Error,
            statusMessage = "Upload failed.",
            statusDetails = throwable.message ?: "unknown error",
            isBusy = false,
            isMetadataLoaded = true,
            isCompleted = false,
        )
    }

    private fun parseUploadStatus(
        line: String,
    ): String? {
        if (!line.startsWith(prefix = STATUS_PREFIX)) return null
        return line.removePrefix(prefix = STATUS_PREFIX)
    }

    private fun parseUploadProgress(
        line: String,
    ): UploadProgressState? {
        if (!line.startsWith(prefix = PROGRESS_PREFIX)) return null

        val payload = line.removePrefix(prefix = PROGRESS_PREFIX)
        val separatorIndex = payload.indexOf(';')
        if (separatorIndex <= 0 || separatorIndex == payload.lastIndex) return null

        val sentBytes = payload.substring(startIndex = 0, endIndex = separatorIndex).toLongOrNull()
            ?: return null
        val totalBytes = payload.substring(startIndex = separatorIndex + 1).toLongOrNull()
            ?: return null
        if (totalBytes <= 0L) return null

        return UploadProgressState(
            sentBytes = sentBytes.coerceAtMost(totalBytes),
            totalBytes = totalBytes,
            percent = ((sentBytes * 100) / totalBytes)
                .toInt()
                .coerceIn(minimumValue = 0, maximumValue = 100),
        )
    }

    private fun UploadAction.focusMessage(): String =
        when (this) {
            UploadAction.Start -> "Upload focused. Press Enter to continue."
            UploadAction.Cancel -> "Cancel focused. Press Enter to abort the flow."
        }

    private fun resolvedTaskName(): String =
        uiState.configuration.taskName.ifBlank { DEFAULT_UPLOAD_TASK_NAME }

    private companion object {
        const val DEFAULT_STATUS_MESSAGE =
            "Use arrow keys to move focus. Press Enter to confirm the selected action."
        const val DEFAULT_UPLOAD_TASK_NAME = "uploadApkForDebug"
        const val STATUS_PREFIX = "UPLOAD_STATUS::"
        const val PROGRESS_PREFIX = "UPLOAD_PROGRESS::"
    }
}

data class UploadAppVersionUiState(
    val configuration: UploadAppVersionConfiguration,
    val selectedAction: UploadAction,
    val confirmedAction: UploadAction?,
    val statusKind: UploadStatusKind,
    val statusMessage: String,
    val statusDetails: String?,
    val uploadProgress: UploadProgressState?,
    val isBusy: Boolean,
    val isMetadataLoaded: Boolean,
    val isCompleted: Boolean,
)

sealed interface UploadAppVersionCommand {
    data object Exit : UploadAppVersionCommand
}

data class UploadAppVersionConfiguration(
    val pluginName: String = "",
    val taskName: String = "",
    val projectName: String = "",
    val buildType: String = "",
    val versionName: String = "",
    val versionCode: String = "",
)

enum class UploadAction {
    Start,
    Cancel,
}

enum class UploadStatusKind {
    Idle,
    Loading,
    Success,
    Error,
}

data class UploadProgressState(
    val sentBytes: Long,
    val totalBytes: Long,
    val percent: Int,
)
