import com.example.klaf.di.dependencies.Modules

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.serialization)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    id("lokdroid-callsite-index") version "0.1.2-alpha9"
}

kotlin {
    jvmToolchain(17)

    jvm("desktop")

    androidTarget()
    val iosX64 = iosX64()
    val iosArm64 = iosArm64()
    val iosSimulatorArm64 = iosSimulatorArm64()

    val frameworkName = "PresentationKit"
    listOf(iosX64, iosArm64, iosSimulatorArm64).forEach { iosTarget ->
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

                implementation(libs.firebase.authentication)

                implementation(platform(libs.compose.bom.get()))
                implementation(libs.compose.runtime)
                implementation(libs.compose.ui)
                implementation(libs.compose.foundation)
                implementation(libs.compose.foundation.layout)
                implementation(libs.compose.ui.tooling)
                implementation(libs.compose.activity)
                implementation(libs.compose.ui.tooling.preview)
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
