plugins {
    `kotlin-dsl`
}

gradlePlugin {
    plugins {
        register("telegram-app-distribution-plugin") {
            id = "telegram-app-distribution-plugin"
            implementationClass = "plugins.telegramAppDistribution.UploadPlugin"
        }

        register("extensions-plugin") {
            id = "extensions-plugin"
            implementationClass = "plugins.MyUtilitiesPlugin"
        }
    }
}

dependencies {
    implementation(libs.agp)
    implementation(libs.kotlin.gradle.plugin)

    // Deliberately not libs.bundles.ktor, which follows the app. This module is `kotlin-dsl`, so it
    // compiles at the Kotlin API version Gradle embeds, and cannot read a Ktor built with a newer
    // one. The upload plugin only posts a file, so it takes its own pinned client instead.
    implementation(libs.buildLogic.ktor.client.core)
    implementation(libs.buildLogic.ktor.client.okhttp)
}
