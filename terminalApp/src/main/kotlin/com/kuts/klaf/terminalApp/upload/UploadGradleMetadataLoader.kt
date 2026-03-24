package com.kuts.klaf.terminalApp.upload

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class UploadGradleMetadataLoader(
    private val projectRoot: File,
) {

    suspend fun load(
        uploadTaskName: String,
    ): Result<UploadAppVersionConfiguration> = withContext(Dispatchers.IO) {
        runCatching {
            val describeTaskName = buildDescribeTaskName(uploadTaskName = uploadTaskName)
            val process = ProcessBuilder(
                "./gradlew",
                ":app:$describeTaskName",
                "--quiet",
                "--console=plain",
            )
                .directory(projectRoot)
                .redirectErrorStream(true)
                .start()

            val outputLines = process.inputStream.bufferedReader().readLines()
            val exitCode = process.waitFor()
            val configuration = parseConfiguration(outputLines = outputLines)

            check(exitCode == 0) {
                buildString {
                    append("Gradle metadata task failed")
                    if (outputLines.isNotEmpty()) {
                        append(": ")
                        append(outputLines.joinToString(separator = " | "))
                    }
                }
            }

            requireNotNull(configuration) {
                "Gradle metadata task produced no upload metadata."
            }
        }
    }

    private fun parseConfiguration(
        outputLines: List<String>,
    ): UploadAppVersionConfiguration? {
        val values = outputLines.mapNotNull { line ->
            if (!line.startsWith(INFO_PREFIX)) return@mapNotNull null

            val payload = line.removePrefix(INFO_PREFIX)
            val separatorIndex = payload.indexOf('=')
            if (separatorIndex <= 0) return@mapNotNull null

            val key = payload.substring(startIndex = 0, endIndex = separatorIndex)
            val value = payload.substring(startIndex = separatorIndex + 1)
            key to value
        }.toMap()

        val taskName = values["taskName"] ?: return null
        return UploadAppVersionConfiguration(
            pluginName = values["pluginName"].orEmpty(),
            taskName = taskName,
            projectName = values["projectName"].orEmpty(),
            buildType = values["buildType"].orEmpty(),
            versionName = values["versionName"].orEmpty(),
            versionCode = values["versionCode"].orEmpty(),
        )
    }

    private fun buildDescribeTaskName(
        uploadTaskName: String,
    ): String {
        require(uploadTaskName.startsWith(prefix = UPLOAD_TASK_PREFIX)) {
            "Unsupported upload task name: $uploadTaskName"
        }

        return uploadTaskName.replaceFirst(
            oldValue = UPLOAD_TASK_PREFIX,
            newValue = DESCRIBE_TASK_PREFIX,
        )
    }

    private companion object {
        const val INFO_PREFIX = "UPLOAD_INFO::"
        const val UPLOAD_TASK_PREFIX = "uploadApkFor"
        const val DESCRIBE_TASK_PREFIX = "describeUploadFor"
    }
}
