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

    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(Modules.Domain))
                implementation(libs.core.coroutines.core)
                implementation(libs.kotlin.serilization)
                implementation(libs.datastore.preferences.core)
                implementation(libs.moko.permissions)
                implementation(libs.moko.permissions.notifications)
                implementation(compose.components.resources)

                implementation(compose.runtime)
                implementation(compose.ui)
                implementation(compose.foundation)
                implementation(compose.material3)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.core.android.ktx)
                implementation(libs.core.app.compat)
                implementation(libs.androidx.material3.android)
                implementation(libs.core.coroutines.core.jvm)

                implementation(libs.navigation.compose)

                implementation(libs.lifecycle.viewmodel.ktx)
                implementation(libs.lifecycle.livedata.ktx)
                implementation(libs.lifecycle.viewmodel.savedstate)

                implementation(libs.koin.android)
                implementation(libs.koin.androidx.compose)

                implementation(libs.firebase.authentication)

                implementation(platform("androidx.compose:compose-bom:${libs.versions.composeBom.get()}"))
                implementation(libs.compose.runtime)
                implementation(libs.compose.ui)
                implementation(libs.compose.foundation)
                implementation(libs.compose.foundation.layout)
                implementation(libs.compose.ui.tooling)
                implementation(libs.compose.activity)
                implementation(libs.compose.ui.tooling.preview)
                implementation(libs.compose.accompanist)

                implementation(libs.cambridge.dictionary.core)
                implementation(libs.cambridge.dictionary.client)
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
