import com.example.klaf.di.dependencies.Modules

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.serialization)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvmToolchain(17)

    jvm("desktop")

    androidTarget()
    // iosX64 (Intel simulator) dropped with Compose Multiplatform 1.11: it is no longer
    // published for that target. Apple Silicon uses iosSimulatorArm64.
    val iosArm64 = iosArm64()
    val iosSimulatorArm64 = iosSimulatorArm64()

    val frameworkName = "PresentationKit"
    listOf(iosArm64, iosSimulatorArm64).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = frameworkName
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(Modules.Domain))
                implementation(libs.core.coroutines.core)
                implementation(libs.kotlin.serilization)
                implementation(libs.core.datetime)
                implementation(libs.datastore.preferences.core)
                implementation(compose.components.resources)
                implementation(libs.compose.multiplatform.tooling.preview)

                implementation(compose.runtime)
                implementation(compose.ui)
                implementation(libs.compose.ui.backhandler)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(libs.navigation.compose)

                implementation(libs.lifecycle.runtime)
                implementation(libs.lifecycle.runtime.compose)
                api(libs.lifecycle.viewmodel)
                implementation(libs.lifecycle.viewmodel.savedstate)

                implementation(libs.koin.compose)
                implementation(libs.koin.compose.viewmodel)

                /** LoKdroid **/
                implementation(libs.lokdroid)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.tests.kotlin)
                implementation(libs.tests.coroutine)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.core.android.ktx)
                implementation(libs.core.app.compat)
                implementation(libs.androidx.material3.android)
                implementation(libs.core.coroutines.core.jvm)

                implementation(libs.lifecycle.viewmodel.ktx)
                implementation(libs.lifecycle.livedata.ktx)
                implementation(libs.lifecycle.viewmodel.compose)

                implementation(libs.koin.android)
                implementation(libs.moko.permissions)
                implementation(libs.moko.permissions.notifications)
                implementation(libs.moko.permissions.microphone)

                implementation(libs.firebase.authentication)

                // KT-58759: a source set's own platform(Any) became an error in Kotlin 2.3, and
                // Gradle's typed one has to be reached through `dependencies` from in here.
                implementation(dependencies.platform(libs.compose.bom))
                implementation(libs.compose.runtime)
                implementation(libs.compose.ui)
                implementation(libs.compose.foundation)
                implementation(libs.compose.foundation.layout)
                implementation(libs.compose.ui.tooling)
                implementation(libs.compose.activity)
                implementation(libs.compose.ui.tooling.preview)

                implementation(libs.lokdroid)
            }
        }
    }
}

compose.resources {
    packageOfResClass = "com.kuts.klaf.presentation.resources"
    publicResClass = true
}

android {
    namespace = "com.kuts.klaf.presentation"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.androidMinSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
        compose = true
    }

}
