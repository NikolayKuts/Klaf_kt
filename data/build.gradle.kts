plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.android.serialization)
}

android {
    namespace = "com.kuts.klaf.data"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.androidMinSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ksp { arg("room.schemaLocation", "$projectDir/schemas") }
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
    // Keep domain contracts visible for KSP/Hilt processing in this module.
    api(project(":domain"))

    /** Core **/
    implementation(libs.core.kotlin.stdlib)
    implementation(libs.core.android.ktx)
    implementation(libs.core.fragment.ktx)
    implementation(libs.core.coroutines.core.jvm)
    implementation(libs.core.javax.inject)

    /** Room **/
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    /** Lifecycle **/
    implementation(libs.lifecycle.livedata.ktx)

    /** Hilt **/
    implementation(libs.hilt.android)
    ksp(libs.hilt.dagger.compiler)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.work)

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
