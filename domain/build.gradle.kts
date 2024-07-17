import com.example.klaf.di.dependencies.Core
import com.example.klaf.di.dependencies.Serialization
import com.example.klaf.di.dependencies.Tests
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.plugin.ide.kotlinExtrasSerialization

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

    /** core **/
    implementation(Core.CoroutinesCoreJvm)
    implementation(Core.JavaxInject)

    /** tests **/
    testImplementation(Tests.JunitCore)
    testImplementation(Tests.Kotlin)
    testImplementation(Tests.Coroutine)

    /** serialization **/
    implementation(Serialization.Core)
}