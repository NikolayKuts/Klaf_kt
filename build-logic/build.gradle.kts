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
    implementation(libs.bundles.ktor)
}
