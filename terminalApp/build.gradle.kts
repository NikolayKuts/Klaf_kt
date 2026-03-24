plugins {
    application
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.compose.compiler)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
}

application {
    mainClass = "com.kuts.klaf.terminalApp.MainKt"
}

dependencies {
    implementation(libs.mosaic.runtime)
    implementation(libs.core.coroutines.core.jvm)
}
