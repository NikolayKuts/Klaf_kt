plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
    alias(libs.plugins.android.serialization)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {

    /** Core **/
    implementation(libs.core.coroutines.core.jvm)
    implementation(libs.core.javax.inject)

    /** Tests **/
    testImplementation(libs.tests.junit.core)
    testImplementation(libs.tests.kotlin)
    testImplementation(libs.tests.coroutine)

    /** Kotlin Serialization **/
    implementation(libs.kotlin.serilization)
}