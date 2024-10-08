package plugins

import org.gradle.api.Project
import java.io.File
import java.util.Properties

fun Project.loadLocalProperties(): Map<String, String> {
    val propertiesFile = File(rootDir, "local.properties")

    if (!propertiesFile.exists()) {
        throw IllegalArgumentException("File \'local.properties\' not found in ${projectDir.path}")
    }

    val properties = Properties()

    propertiesFile.inputStream().use { inputStream -> properties.load(inputStream) }

    return properties.entries.associate { it.key.toString() to it.value.toString() }
}
