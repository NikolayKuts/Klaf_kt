import com.example.klaf.di.dependencies.Modules

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.navigation.safeargs.kotlin)
    alias(libs.plugins.android.serialization)
    alias(libs.plugins.compose.compiler)
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

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        compose = true
    }

}

dependencies {
    implementation(project(Modules.Domain))

    /** Core **/
    implementation(libs.core.kotlin.stdlib)
    implementation(libs.core.android.ktx)
    implementation(libs.core.app.compat)
    implementation(libs.core.legacy.support)
    implementation(libs.core.fragment.ktx)
    implementation(libs.androidx.material3.android)
    implementation(libs.core.coroutines.core.jvm)

    /** Navigation **/
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)
    implementation(libs.navigation.dynamic.features.fragment)

    /** Lifecycle **/
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.savedstate)

    /** Hilt **/
    implementation(libs.hilt.android)
    ksp(libs.hilt.dagger.compiler)
    ksp(libs.hilt.android.compiler)

    /** Firebase **/
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.authentication)

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

    /** Kotlin Serialization **/
    implementation(libs.kotlin.serilization)

    /** SplashScreen API **/
    implementation(libs.androidx.core.splashscreen)

    /** CambridgeLib **/
    implementation(libs.cambridge.dictionary.core)
    implementation(libs.cambridge.dictionary.client)

    /** LoKdroid **/
    implementation(libs.lokdroid)
}
