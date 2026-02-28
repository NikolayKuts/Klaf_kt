import com.example.klaf.di.dependencies.Modules
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.android.serialization)
    alias(libs.plugins.room)
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
                implementation(project(Modules.Domain))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${libs.versions.coroutinesCoreJvm.get()}")
                implementation(libs.kotlin.serilization)
                implementation(libs.ktor.client.core)
                implementation(libs.room.runtime)
                implementation(libs.sqlite.bundled)
            }
        }
        androidMain {
            dependencies {
                implementation(libs.core.android.ktx)

                implementation(libs.lifecycle.livedata.ktx)

                implementation(libs.koin.core)

                implementation(libs.firebase.rirestore.ktx)
                implementation(libs.firebase.authentication)
                implementation(libs.firebase.coroutine.play.services)
                implementation(libs.firebase.crashlytics)

                implementation(libs.work.manager)

                implementation(libs.datastore.android)

                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.ktor.client.okhttp)
                implementation(libs.ktor.client.cio)
                implementation(libs.ktor.client.content.negotiation)

                implementation(libs.lokdroid)
            }
        }
    }
}

room {
    generateKotlin = true
    schemaDirectory("$projectDir/schemas")
}

android {
    namespace = "com.kuts.klaf.data"
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
    setOf(
        "kspCommonMainMetadata",
        "kspAndroid",
        "kspIosX64",
        "kspIosArm64",
        "kspIosSimulatorArm64",
    ).forEach { configName ->
        add(configName, libs.room.compiler)
    }
}
