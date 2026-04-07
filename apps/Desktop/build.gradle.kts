import com.example.klaf.di.dependencies.Modules

plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    id("lokdroid-callsite-index") version "0.1.2-alpha9"
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(Modules.Di))
    implementation(project(Modules.Presentation))
    implementation(libs.koin.core)
    implementation(libs.core.coroutines.swing)
    implementation(libs.core.datetime)
    implementation(compose.desktop.currentOs)

    /** LoKdroid **/
    implementation(libs.lokdroid)
}

compose.desktop {
    application {
        mainClass = "com.kuts.klaf.desktop.MainKt"
    }
}
