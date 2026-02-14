pluginManagement {
    includeBuild("build-logic")

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

val localProperties = java.util.Properties().apply {
    val localPropertiesFile = rootDir.resolve("local.properties")
    if (localPropertiesFile.exists()) {
        load(localPropertiesFile.inputStream())
    }
}

fun getLocalProperty(key: String): String =
    localProperties.getProperty(key) ?: error("Missing local.properties key: $key")

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        mavenLocal()

        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/NikolayKuts/Web-Companion")

            credentials {
                username = getLocalProperty(key = "canbride.dictionary.client.username")
                password = getLocalProperty(key = "canbride.dictionary.client.password")
            }
        }
    }
}

rootProject.name = "Klaf"
include(":app")
include(":domain")
include(":shared")
include(":data")
include(":presentation")
