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

            val taskName = variant.buildTaskName()
            val apkVariantDiractoryProvider: Provider<Directory> = variant.artifacts.get(
                SingleArtifact.APK
            )

            project.tasks.register(taskName, UploadTask::class.java) {
                val data = ApkData(
                    versionCode = variantCodes.firstOrNull()?.toString() ?: "",
                    versionName = variantNames.firstOrNull() ?: "",
                    buildType = variant.buildType.toString()
                )

                apkData = data
                apkDirectoryProperty.set(apkVariantDiractoryProvider)

                println("ApkData = ${data}")
            }
        }
    }

    private fun ApplicationVariant.buildTaskName(): String {
        val variantName = this.name.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
        }

        return "$TASK_NAME_PREFIX$variantName"
    }
}