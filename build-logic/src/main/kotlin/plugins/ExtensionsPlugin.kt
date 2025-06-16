package plugins

import org.gradle.api.Plugin
import org.gradle.api.Project


class MyUtilitiesPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val props = loadLocalProperties(project.rootDir)
        val githubUser = props.getProperty("github.user") ?: "defaultUser"

        // Example: define an extra property available to build scripts
        project.extensions.extraProperties["githubUser"] = githubUser
    }
}