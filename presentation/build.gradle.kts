import com.example.klaf.di.dependencies.Modules

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.navigation.safeargs.kotlin)
    alias(libs.plugins.android.serialization)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvmToolchain(17)

    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val androidMain by getting {
            dependencies {
                implementation(project(Modules.Domain))

                implementation(libs.core.android.ktx)
                implementation(libs.core.app.compat)
                implementation(libs.core.legacy.support)
                implementation(libs.core.fragment.ktx)
                implementation(libs.androidx.material3.android)
                implementation(libs.core.coroutines.core.jvm)

                implementation(libs.navigation.fragment.ktx)
                implementation(libs.navigation.ui.ktx)
                implementation(libs.navigation.dynamic.features.fragment)

                implementation(libs.lifecycle.viewmodel.ktx)
                implementation(libs.lifecycle.livedata.ktx)
                implementation(libs.lifecycle.viewmodel.savedstate)

                implementation(libs.koin.android)
                implementation(libs.koin.androidx.navigation)

                implementation(libs.firebase.authentication)

                implementation(platform("androidx.compose:compose-bom:${libs.versions.composeBom.get()}"))
                implementation(libs.compose.compiler)
                implementation(libs.compose.runtime)
                implementation(libs.compose.ui)
                implementation(libs.compose.foundation)
                implementation(libs.compose.foundation.layout)
                implementation(libs.compose.material)
                implementation(libs.compose.runtime.livedata)
                implementation(libs.compose.ui.tooling)
                implementation(libs.compose.activity)
                implementation(libs.compose.ui.tooling.preview)
                implementation(libs.compose.theme.adapter)
                implementation(libs.compose.accompanist)

                implementation(libs.kotlin.serilization)
                implementation(libs.androidx.core.splashscreen)
                implementation(libs.cambridge.dictionary.core)
                implementation(libs.cambridge.dictionary.client)
                implementation(libs.lokdroid)
            }
        }
    }
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
