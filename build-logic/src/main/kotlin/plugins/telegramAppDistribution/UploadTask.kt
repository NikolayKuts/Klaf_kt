package plugins.telegramAppDistribution

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.BodyProgress
import kotlinx.coroutines.runBlocking
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction
import plugins.loadLocalProperties
import java.io.File

abstract class UploadTask : DefaultTask() {

    companion object {

        private const val APK_FILE_EXTENSION = ".apk"
    private const val APP_POINTER = "-app-"
    }

    @get:InputDirectory
    abstract val apkDirectoryProperty: DirectoryProperty

    @get:Input
    abstract var apkData: ApkData

    @TaskAction
    fun upload() {
        val api = TelegramApi(
            HttpClient(OkHttp) {
                install(BodyProgress)
            }
        )
        val localProperties = project.loadLocalProperties()

        runBlocking {
            apkDirectoryProperty.get().asFile.listFiles()
                ?.filter { it.name.endsWith(APK_FILE_EXTENSION) }
                ?.forEach { apkFile ->
                    UploadConsoleEvent.status("Preparing APK...")
                    val newFileName = buildFileName(
                        projectName = project.rootProject.name,
                        apkData = apkData
                    )
                    val newFile = File(apkFile.parentFile, newFileName)
                    val renamed = apkFile.renameTo(newFile)

                    if (renamed) {
                        UploadConsoleEvent.status("Uploading ${newFile.name}")
                        var lastPercent = -1
                        api.uploadFile(
                            file = newFile,
                            token = localProperties["telegramApi.token"] ?: "",
                            chatId = localProperties["telegramApi.chatId"] ?: "",
                        ) { sentBytes, totalBytes ->
                            if (totalBytes <= 0L) return@uploadFile

                            val percent = ((sentBytes * 100) / totalBytes)
                                .toInt()
                                .coerceIn(minimumValue = 0, maximumValue = 100)
                            if (percent != lastPercent) {
                                lastPercent = percent
                                UploadConsoleEvent.progress(
                                    sentBytes = sentBytes,
                                    totalBytes = totalBytes,
                                )
                            }
                        }
                        UploadConsoleEvent.status("Upload completed.")
                    } else {
                        UploadConsoleEvent.status("APK rename failed.")
                    }
                }
        }
    }

    private fun buildFileName(
        projectName: String,
        apkData: ApkData
    ): String = buildString {
        append(projectName)
        append(APP_POINTER)
        append(apkData.buildType)
        append("-")
        append(apkData.versionName)
        append("(${apkData.versionCode})")
        append(APK_FILE_EXTENSION)
    }
}
