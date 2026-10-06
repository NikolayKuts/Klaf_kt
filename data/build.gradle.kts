import com.example.klaf.di.dependencies.Modules
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.android.serialization)
    alias(libs.plugins.room)
}

kotlin {
    jvmToolchain(17)

    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    // iosX64 (Intel simulator) dropped with Compose Multiplatform 1.11: it is no longer
    // published for that target. Apple Silicon uses iosSimulatorArm64.
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(Modules.KlafServerContract))
                implementation(project(Modules.Domain))
                implementation(libs.core.coroutines.core)
                implementation(libs.kotlin.serilization)
                implementation(libs.datastore.core)
                implementation(libs.datastore.preferences.core)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.websockets)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.room.runtime)
                implementation(libs.sqlite.bundled)

                /** LoKdroid **/
                implementation(libs.lokdroid)

            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.tests.kotlin)
            }
        }

        val iosMain = maybeCreate("iosMain").apply {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }
        val iosArm64Main by getting { dependsOn(iosMain) }
        val iosSimulatorArm64Main by getting { dependsOn(iosMain) }

        val desktopMain by getting {
            dependencies {
                implementation(libs.ktor.client.cio)
                implementation("net.java.dev.jna:jna-platform:5.19.1")
            }
        }
        val desktopTest by getting {
            dependencies {
                implementation("io.ktor:ktor-client-mock:3.5.1")
                implementation(libs.ktor.server.core)
                implementation(libs.ktor.server.cio)
                implementation(libs.ktor.server.websockets)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.core.android.ktx)

                implementation(libs.lifecycle.livedata.ktx)

                implementation(libs.koin.core)

                implementation(libs.firebase.rirestore.ktx)
                implementation(libs.firebase.authentication)
                implementation(libs.firebase.coroutine.play.services)
                implementation(libs.firebase.crashlytics)
                implementation(libs.firebase.storage)
                implementation(libs.firebase.messaging)

                implementation(libs.work.manager)

                implementation(libs.ktor.client.okhttp)

                implementation(libs.lokdroid)
            }
        }
        val androidInstrumentedTest by getting {
            dependencies {
                implementation(libs.tests.kotlin)
                implementation(libs.tests.junit.android)
                implementation(libs.androidx.runner)
                implementation(libs.androidx.core)
            }
        }
    }
}

room {
    generateKotlin = true
    schemaDirectory("$projectDir/schemas")
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
}

dependencies {
    add("androidMainImplementation", platform(libs.firebase.bom))

    setOf(
        "kspCommonMainMetadata",
        "kspDesktop",
        "kspAndroid",
        "kspIosArm64",
        "kspIosSimulatorArm64",
    ).forEach { configName ->
        add(configName, libs.room.compiler)
    }
}
