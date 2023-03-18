package com.example.klaf.di.dependencies

object Modules {

    const val Domain = ":domain"
}

object Core {

    const val Core = "androidx.core:core-ktx:${Versions.AndroidxCore}"
    const val AppCompat = "androidx.appcompat:appcompat:${Versions.AndroidxAppCompat}"
    const val LegacySupport = "androidx.legacy:legacy-support-v4:${Versions.AndroidxLegacySupport}"
    const val ConstraintLayout = "androidx.constraintlayout:constraintlayout:${Versions.AndroidxConstraintLayout}"
    const val Fragment = "androidx.fragment:fragment-ktx:${Versions.AndroidxFragmentKtx}"
    const val Material = "com.google.android.material:material:${Versions.Material}"
    const val CoroutinesCoreJvm = "org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:${Versions.CoroutinesCoreJvm}"
    const val JavaxInject = "javax.inject:javax.inject:${Versions.JavaxInject}"
}

object Navigation {

    const val FragmentKtx = "androidx.navigation:navigation-fragment-ktx:${Versions.NavigationCore}"
    const val UiKtx = "androidx.navigation:navigation-ui-ktx:${Versions.NavigationCore}"
    const val DynamicFeaturesFragment = "androidx.navigation:navigation-dynamic-features-fragment:${Versions.NavigationCore}"
}

object Kotlin {

    const val Stdlib = "org.jetbrains.kotlin:kotlin-stdlib:${Versions.Kotlin}"
}

object Tests {

    const val JunitCore = "junit:junit:${Versions.JunitCore}"
    const val Mockk = "io.mockk:mockk:${Versions.MockkCore}"
    const val Espresso = "androidx.test.espresso:espresso-core:${Versions.EspressoCore}"
    const val JunitAndroid = "androidx.test.ext:junit:${Versions.JunitAndroid}"
    const val Coroutine = "org.jetbrains.kotlinx:kotlinx-coroutines-test:${Versions.CoroutineTest}"
    const val Kotlin = "org.jetbrains.kotlin:kotlin-test"
    const val Turbine = "app.cash.turbine:turbine:${Versions.Turbine}"
}

object Room {

    const val Runtime = "androidx.room:room-runtime:${Versions.RoomCore}"
    const val Ktx = "androidx.room:room-ktx:${Versions.RoomCore}"
    const val Compiler = "androidx.room:room-compiler:${Versions.RoomCore}"
}

object Lifecycle {

    const val ViewModelKtx = "androidx.lifecycle:lifecycle-viewmodel-ktx:${Versions.Lifecycle}"
    const val LiveDataKtx = "androidx.lifecycle:lifecycle-livedata-ktx:${Versions.Lifecycle}"
    const val ViewModelSavedState = "androidx.lifecycle:lifecycle-viewmodel-savedstate:${Versions.Lifecycle}"
}

object Hilt {

    const val Android = "com.google.dagger:hilt-android:${Versions.HiltAndroid}"
    const val DaggerCompiler = "com.google.dagger:hilt-compiler:${Versions.HiltDaggerCompiler}"
    const val AndroidCompiler = "androidx.hilt:hilt-compiler:${Versions.HiltAndroidCompiler}"
    const val Work = "androidx.hilt:hilt-work:${Versions.HiltWork}"
}

object Firebase {

    const val Bom = "com.google.firebase:firebase-bom:${Versions.FirebaseBom}"
    const val AnalyticsKts = "com.google.firebase:firebase-analytics-ktx"
    const val FirestoreKtx = "com.google.firebase:firebase-firestore-ktx"
    const val CoroutinePlayServices = "org.jetbrains.kotlinx:kotlinx-coroutines-play-services:${Versions.FirebaseCoroutinePlayServices}"
    const val Authentication = "com.google.firebase:firebase-auth-ktx"
}

object Compose {

    const val Bom = "androidx.compose:compose-bom:${Versions.ComposeBom}"
    const val Compiler = "androidx.compose.compiler:compiler:${Versions.ComposeCompiler}"
    const val Runtime = "androidx.compose.runtime:runtime"
    const val Ui = "androidx.compose.ui:ui"
    const val Foundation = "androidx.compose.foundation:foundation"
    const val FoundationLayout = "androidx.compose.foundation:foundation-layout"
    const val Material = "androidx.compose.material:material"
    const val RuntimeLivedata = "androidx.compose.runtime:runtime-livedata"
    const val UiTooling = "androidx.compose.ui:ui-tooling"
    const val Activity = "androidx.activity:activity-compose:1.6.1"
    const val UiToolingPreview = "androidx.compose.ui:ui-tooling-preview"
    const val ConstraintLayout = "androidx.constraintlayout:constraintlayout-compose:${Versions.ComposeConstraintLayout}"
    const val ThemeAdapter = "com.google.android.material:compose-theme-adapter:${Versions.ComposeThemeAdapter}"
    const val Accompanist = "com.google.accompanist:accompanist-swiperefresh:${Versions.ComposeAccompanist}"
}

object WorkManager {

    const val Core = "androidx.work:work-runtime-ktx:${Versions.WorkManagerCore}"
}

object Serialization {

    const val Core = "org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.SerializationCore}"
}

object DataStore {

    const val Core = "androidx.datastore:datastore:${Versions.DataStoreCore}"
}

object Retrofit {

    const val Core =  "com.squareup.retrofit2:retrofit:${Versions.RetrofitCore}"
}




