import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.android.serialization)
}

kotlin {
    jvmToolchain(17)

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${libs.versions.coroutinesCoreJvm.get()}")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
                implementation(libs.kotlin.serilization)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.tests.kotlin)
                implementation(libs.tests.coroutine)
            }
        }
    }
}

android {
    namespace = "com.kuts.domain"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.androidMinSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
