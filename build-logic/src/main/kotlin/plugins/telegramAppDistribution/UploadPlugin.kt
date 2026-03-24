package plugins.telegramAppDistribution

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.ApplicationVariant
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import java.util.Locale

/**
 * ./gradlew uploadApkForDebug --no-build-cache -s
 */
class UploadPlugin : Plugin<Project> {

    companion object {

        private const val TASK_NAME_PREFIX = "uploadApkFor"
        private const val DESCRIBE_TASK_NAME_PREFIX = "describeUploadFor"
        private const val PLUGIN_NAME = "telegram-app-distribution-plugin"
    }

    override fun apply(project: Project) {
        val androidComponents = project.extensions.findByType(
            ApplicationAndroidComponentsExtension::class.java
        ) ?: throw IllegalStateException("android plugin not found!")

        androidComponents.onVariants { variant ->
            val variantNames = variant.outputs.map { variantOutput ->
                variantOutput.versionName.get()
            }
            val variantCodes = variant.outputs.map { variantOutput ->
                variantOutput.versionCode.get()
            }

            val taskName = variant.buildTaskName(prefix = TASK_NAME_PREFIX)
            val describeTaskName = variant.buildTaskName(prefix = DESCRIBE_TASK_NAME_PREFIX)
            val apkVariantDiractoryProvider: Provider<Directory> = variant.artifacts.get(
                SingleArtifact.APK
            )
            val data = ApkData(
                versionCode = variantCodes.firstOrNull()?.toString() ?: "",
                versionName = variantNames.firstOrNull() ?: "",
                buildType = variant.buildType.toString()
            )

            project.tasks.register(taskName, UploadTask::class.java) {
                apkData = data
                apkDirectoryProperty.set(apkVariantDiractoryProvider)
            }

            project.tasks.register(describeTaskName, DescribeUploadTask::class.java) {
                pluginName = PLUGIN_NAME
                uploadTaskName = taskName
                apkData = data
            }
        }
    }

    private fun ApplicationVariant.buildTaskName(prefix: String): String {
        val variantName = this.name.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
        }

        return "$prefix$variantName"
    }
}
