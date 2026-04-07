import com.example.klaf.di.dependencies.Modules
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    id("lokdroid-callsite-index") version "0.1.2-alpha9"
}

kotlin {
    jvmToolchain(17)

    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    val iosX64 = iosX64()
    val iosArm64 = iosArm64()
    val iosSimulatorArm64 = iosSimulatorArm64()

    val frameworkName = "KlafAppKit"
    listOf(iosX64, iosArm64, iosSimulatorArm64).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = frameworkName
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(Modules.Domain))
                implementation(project(Modules.Data))
                implementation(project(Modules.Presentation))
                implementation(libs.datastore.preferences.core)
                implementation(libs.koin.core)
                implementation(libs.ktor.client.core)
            }
        }

        val iosMain = maybeCreate("iosMain").apply {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.koin.compose.viewmodel)
            }
        }
        val iosX64Main by getting { dependsOn(iosMain) }
        val iosArm64Main by getting { dependsOn(iosMain) }
        val iosSimulatorArm64Main by getting { dependsOn(iosMain) }

        val desktopMain by getting {
            dependencies {
                implementation(libs.koin.compose.viewmodel)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.core.coroutines.core.jvm)
                implementation(libs.work.manager)
                implementation(libs.datastore.android)
                implementation(libs.datastore.preferences.android)
                implementation(libs.room.runtime)

                implementation(libs.firebase.rirestore.ktx)
                implementation(libs.firebase.authentication)
                implementation(libs.firebase.crashlytics)

                implementation(libs.cambridge.dictionary.client)
                implementation(libs.cambridge.dictionary.core)
                implementation(libs.lokdroid)

                implementation(libs.koin.android)
                implementation(libs.koin.androidx.workmanager)
            }
        }
    }
}

android {
    namespace = "com.kuts.klaf.di"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.androidMinSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    add("androidMainImplementation", platform(libs.firebase.bom))
}
