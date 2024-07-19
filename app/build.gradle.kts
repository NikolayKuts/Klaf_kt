import com.example.klaf.di.dependencies.*

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
    alias(libs.plugins.hilt.android)
    id("kotlin-parcelize")
    alias(libs.plugins.navigation.safeargs.kotlin)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.android.serialization)
}

android {
    defaultConfig {
        applicationId = "com.kuts.klaf"
        compileSdk = 34
        minSdk = 21
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        kapt {
            arguments { arg("room.schemaLocation", "$projectDir/schemas") }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.kotlinCompilerExtension.get()
    }
    namespace = "com.kuts.klaf"
}

dependencies {

    /** modules **/
    implementation(project(Modules.Domain))

    /** core **/
    implementation(Kotlin.Stdlib)
    implementation(Core.Core)
    implementation(Core.AppCompat)
    implementation(Core.ConstraintLayout)
    implementation(Core.LegacySupport)
    implementation(Core.Fragment)
    implementation(Core.Material)
    implementation(Core.Preferences)

    /** tests **/
    testImplementation(Tests.JunitCore)
    androidTestImplementation(Tests.JunitAndroid)
    androidTestImplementation(Tests.Espresso)
    testImplementation(Tests.Mockk)
    testImplementation(Tests.Coroutine)
    testImplementation(Tests.Turbine)
    testImplementation(Tests.Kotlin)

    /** navigation **/
    implementation(Navigation.FragmentKtx)
    implementation(Navigation.UiKtx)
    implementation(Navigation.DynamicFeaturesFragment)

    /** room **/
    implementation(Room.Runtime)
    implementation(Room.Ktx)
    kapt(Room.Compiler)

    /** lifecycle **/
    implementation(Lifecycle.ViewModelKtx)
    implementation(Lifecycle.LiveDataKtx)
    implementation(Lifecycle.ViewModelSavedState)

    /** hilt **/
    implementation(Hilt.Android)
    kapt(Hilt.DaggerCompiler)
    kapt(Hilt.AndroidCompiler)
    implementation(Hilt.Work)

    /** firebase **/
    implementation(platform(Firebase.Bom))
    implementation(Firebase.AnalyticsKts)
    implementation(Firebase.FirestoreKtx)
    implementation(Firebase.Authentication)
    implementation(Firebase.CoroutinePlayServices)
    implementation(Firebase.Crashlytics)

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
    implementation(libs.compose.constraintLayout)
    implementation(libs.compose.theme.adapter)
    implementation(libs.compose.accompanist)

    /** Work Manager **/
    implementation(libs.work.manager)

    /** Kotlin serialization **/
    implementation(libs.kotlin.serilization)

    /** DataStore **/
    implementation(libs.datastore.android)

    /** Retrofit **/
    implementation(libs.retrofit.core)
}