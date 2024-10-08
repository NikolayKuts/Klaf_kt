plugins {
    `kotlin-dsl`
}

gradlePlugin {
    plugins {
        register("telegram-app-distribution-plugin") {
            id = "telegram-app-distribution-plugin"
            implementationClass = "plugins.telegramAppDistribution.UploadPlugin"
        }
    }
}

dependencies {
    implementation(libs.agp)
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.bundles.ktor)
}
