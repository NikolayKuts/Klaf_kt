import com.example.klaf.di.dependencies.*

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.android.serialization)
    alias(libs.plugins.telegramAppDistribution)
    alias(libs.plugins.compose.compiler)
    id("extensions-plugin")
}

android {
    defaultConfig {
        applicationId = "com.kuts.klaf"
        compileSdk = libs.versions.androidCompileSdk.get().toInt()
        minSdk = libs.versions.androidMinSdk.get().toInt()
        targetSdk = 33

        /** Version **/
        versionName = "1.5"
        versionCode = 15

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

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        compose = true
    }

    namespace = "com.kuts.klaf"
}

dependencies {

    /** Modules **/
    implementation(project(Modules.Di))
    implementation(project(Modules.Presentation))

    /** Core **/
    implementation(libs.core.kotlin.stdlib)
    implementation(libs.core.app.compat)

    /** Tests **/
    testImplementation(libs.tests.junit.core)
    testImplementation(libs.junit.jupiter)
    androidTestImplementation(libs.tests.junit.android)
    androidTestImplementation(libs.tests.espresso)

    /** Koin **/
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.workmanager)

    /** Compose **/
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.foundation.layout)
    implementation(libs.compose.activity)

    /** LoKdroid **/
    implementation(libs.lokdroid)

    /** SplashScreen API **/
    implementation(libs.androidx.core.splashscreen)
}
