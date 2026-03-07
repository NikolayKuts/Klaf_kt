import com.example.klaf.di.dependencies.Modules
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
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
        val commonMain by getting {
            dependencies {
                implementation(project(Modules.Domain))
                implementation(project(Modules.Data))
                implementation(libs.koin.core)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(project(Modules.Presentation))

                implementation(libs.core.coroutines.core.jvm)
                implementation(libs.work.manager)
                implementation(libs.datastore.android)
                implementation(libs.datastore.preferences.android)
                implementation(libs.room.runtime)
                implementation(libs.ktor.client.core)

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
