import com.example.klaf.di.dependencies.Modules

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
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

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    api(project(Modules.Domain))
    api(project(Modules.Data))
    api(project(Modules.Presentation))

    implementation(libs.core.coroutines.core.jvm)
    implementation(libs.work.manager)
    implementation(libs.datastore.android)
    implementation(libs.room.runtime)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.rirestore.ktx)
    implementation(libs.firebase.authentication)
    implementation(libs.firebase.crashlytics)

    implementation(libs.cambridge.dictionary.client)
    implementation(libs.lokdroid)

    implementation(libs.hilt.android)
    implementation(libs.hilt.work)
    ksp(libs.hilt.dagger.compiler)
    ksp(libs.hilt.android.compiler)
}
