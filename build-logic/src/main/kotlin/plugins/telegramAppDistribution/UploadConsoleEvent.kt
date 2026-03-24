package plugins.telegramAppDistribution

internal object UploadConsoleEvent {

    private const val STATUS_PREFIX = "UPLOAD_STATUS::"
    private const val PROGRESS_PREFIX = "UPLOAD_PROGRESS::"

    fun status(
        message: String,
    ) {
        println("$STATUS_PREFIX$message")
    }

    fun progress(
        sentBytes: Long,
        totalBytes: Long,
    ) {
        println("$PROGRESS_PREFIX$sentBytes;$totalBytes")
    }
}
