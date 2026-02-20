import com.example.klaf.di.dependencies.Modules

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.android.serialization)
    alias(libs.plugins.room)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
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

    kotlinOptions {
        jvmTarget = "17"
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(project(Modules.Domain))

    /** Core **/
    implementation(libs.core.kotlin.stdlib)
    implementation(libs.core.android.ktx)
    implementation(libs.core.fragment.ktx)
    implementation(libs.core.coroutines.core.jvm)

    /** Room **/
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    /** Lifecycle **/
    implementation(libs.lifecycle.livedata.ktx)

    /** Koin **/
    implementation(libs.koin.android)

    /** Firebase **/
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.rirestore.ktx)
    implementation(libs.firebase.authentication)
    implementation(libs.firebase.coroutine.play.services)
    implementation(libs.firebase.crashlytics)

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
}
