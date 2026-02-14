import com.example.klaf.di.dependencies.*

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.parcelize)
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

        ksp { arg("room.schemaLocation", "$projectDir/schemas") }
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
    implementation(project(Modules.Shared))

    /** Core **/
    implementation(libs.core.kotlin.stdlib)
    implementation(libs.core.android.ktx)
    implementation(libs.core.app.compat)
    implementation(libs.core.legacy.support)
    implementation(libs.core.fragment.ktx)
    implementation(libs.androidx.material3.android)

    /** Tests **/
    testImplementation(libs.tests.junit.core)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter)
    androidTestImplementation(libs.tests.junit.android)
    androidTestImplementation(libs.tests.espresso)
    testImplementation(libs.tests.mockk)
    testImplementation(libs.tests.coroutine)
    testImplementation(libs.tests.turbine)
    testImplementation(libs.tests.kotlin)

    /** Navigation **/
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)
    implementation(libs.navigation.dynamic.features.fragment)

    /** Room **/
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    /** Lifecycle **/
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.savedstate)

    /** Hilt **/
    implementation(libs.hilt.android)
    ksp(libs.hilt.dagger.compiler)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.work)

    /** Firebase **/
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics.kts)
    implementation(libs.firebase.rirestore.ktx)
    implementation(libs.firebase.authentication)
    implementation(libs.firebase.coroutine.play.services)
    implementation(libs.firebase.crashlytics)

    /** Compose **/
    implementation(platform(libs.compose.bom))
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

    /** Work Manager **/
    implementation(libs.work.manager)

    /** Kotlin Serialization **/
    implementation(libs.kotlin.serilization)

    /** DataStore **/
    implementation(libs.datastore.android)

    /** Ktor **/
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)

    /** LoKdroid **/
    implementation(libs.lokdroid)

    /** SplashScreen API **/
    implementation(libs.androidx.core.splashscreen)

    /** CambridgeLib **/
    implementation(libs.cambridge.dictionary.core)
    implementation(libs.cambridge.dictionary.client)
}
