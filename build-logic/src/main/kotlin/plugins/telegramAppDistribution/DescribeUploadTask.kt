package plugins.telegramAppDistribution

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

abstract class DescribeUploadTask : DefaultTask() {

    companion object {
        const val OUTPUT_PREFIX = "UPLOAD_INFO::"
    }

    @get:Input
    abstract var pluginName: String

    @get:Input
    abstract var uploadTaskName: String

    @get:Input
    abstract var apkData: ApkData

    @TaskAction
    fun describe() {
        printValue(key = "pluginName", value = pluginName)
        printValue(key = "taskName", value = uploadTaskName)
        printValue(key = "projectName", value = project.rootProject.name)
        printValue(key = "buildType", value = apkData.buildType)
        printValue(key = "versionName", value = apkData.versionName)
        printValue(key = "versionCode", value = apkData.versionCode)
    }

    private fun printValue(
        key: String,
        value: String,
    ) {
        println("$OUTPUT_PREFIX$key=$value")
    }
}
