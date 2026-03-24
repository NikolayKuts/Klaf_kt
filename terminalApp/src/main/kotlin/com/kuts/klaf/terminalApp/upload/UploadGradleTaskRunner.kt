package com.kuts.klaf.terminalApp.upload

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class UploadGradleTaskRunner(
    private val projectRoot: File,
) {

    suspend fun runUploadTask(
        taskName: String,
        onOutputLine: (String) -> Unit,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val process = ProcessBuilder(
                "./gradlew",
                ":app:$taskName",
                "--console=plain",
                "--quiet",
                "--warning-mode=none",
            )
                .directory(projectRoot)
                .redirectErrorStream(true)
                .start()

            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    if (line.isNotBlank()) {
                        onOutputLine(line)
                    }
                }
            }

            val exitCode = process.waitFor()
            check(exitCode == 0) {
                "Gradle upload task failed with exit code $exitCode."
            }
        }
    }
}
