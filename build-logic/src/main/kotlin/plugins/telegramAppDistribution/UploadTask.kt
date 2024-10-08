package plugins.telegramAppDistribution

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
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
        val api = TelegramApi(HttpClient(OkHttp))
        val localProperties = project.loadLocalProperties()

        runBlocking {
            apkDirectoryProperty.get().asFile.listFiles()
                ?.onEach { println("File -> ${it.name}") }
                ?.filter { it.name.endsWith(APK_FILE_EXTENSION) }
                ?.forEach { apkFile ->
                    val newFileName = buildFileName(
                        projectName = project.rootProject.name,
                        apkData = apkData
                    )
                    val newFile = File(apkFile.parentFile, newFileName)
                    val renamed = apkFile.renameTo(newFile)

                    if (renamed) {
                        println("File renamed successfully to ${newFile.name}")
                        api.uploadFile(
                            file = newFile,
                            token = localProperties["telegramApi.token"] ?: "",
                            chatId = localProperties["telegramApi.chatId"] ?: "")
                    } else {
                        println("File renamed failed")
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