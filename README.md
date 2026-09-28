<p align="center">
  <img src="preview/Klaf_icon_128px.png" alt="Klaf app icon" />
</p>

# Klaf

Klaf is a Kotlin Multiplatform vocabulary trainer focused on spaced repetition and mnemonic-based memorization.

The repository contains Android, Desktop, and iOS apps that share the same `domain`, `data`, `presentation`, and `di` layers.

Android currently has the most complete platform integration. Desktop and iOS already reuse the shared UI and core logic, but some platform services are still stubbed or limited compared with Android.

## Features

- Deck and card management for vocabulary study
- Spaced repetition review flow
- Word pronunciation playback
- Word autocomplete suggestions
- AI-generated mnemonic text and illustrations
- Dark and light themes
- Shared Compose UI across platforms
- Android background mnemonic generation with progress and result notifications
- Android-specific integrations for notifications, Firebase-backed services, and smart text selection

## Project Structure

- `apps/Android` - Android application module and Android resources
- `apps/Desktop` - Compose Desktop launcher
- `apps/iOS` - Xcode project and SwiftUI host for the iOS app
- `domain` - core entities, use cases, and repository contracts
- `data` - persistence, networking, and repository implementations
- `presentation` - Compose Multiplatform UI, navigation, and view models
- `di` - platform-specific dependency wiring
- `build-logic` - custom Gradle plugins and build logic
- `preview` - screenshots and GIF previews used in the README

## Tech Stack

- Kotlin Multiplatform
- Compose Multiplatform for shared UI
- Kotlin Coroutines and Flow
- Koin for dependency injection
- Room and DataStore for local persistence
- Ktor and Kotlinx Serialization for networking
- Agent Driver client SDK for word insights and mnemonic generation
- Firebase Authentication, Firestore, and Crashlytics on Android
- WorkManager on Android
- Gradle Kotlin DSL with included build logic in `build-logic`

## Platform Status

- Android is the primary app target and has the broadest platform integration.
- Desktop reuses the shared UI and local data stack, but several platform services are development-oriented or no-op.
- iOS runs the shared Compose UI through a SwiftUI host, but some integrations are still intentionally minimal.

Word insights and mnemonic generation use one shared Agent Driver client SDK session. Klaf does
not contain a second AI provider or a runtime provider switch. The Android mnemonic flow uses a foreground service while text or image
generation is active. If the WebSocket drops while Android freezes the app, the
Agent Driver SDK resumes the same logical one-shot request after connectivity
returns. Desktop and iOS currently use a no-op implementation of the shared
background-generation contract.

See [Mnemonic Generation](docs/mnemonic-generation.md) for the component flow,
cleanup rules, platform limits, and diagnostics.

## Requirements

- JDK 17
- Android Studio or IntelliJ IDEA for Android/Desktop work
- Xcode for iOS work
- Android SDK for the Android app

## Run

### Local configuration and privacy

Keep actual `local.properties`, Android `google-services.json`, server service
credentials and private handoff notes outside Git. Use only dummy credentials
in test fixtures and generic paths/accounts in documentation. The ignored
`.local-notes/` directory is machine-local and must never be force-added.

### Android

Use Android Studio, or run:

```bash
./gradlew :Android:installDebug
```

### Desktop

Run the desktop app with:

```bash
./gradlew :Desktop:run
```

### iOS

Open the Xcode project and run the `iOS` scheme:

```text
apps/iOS/iosApp.xcodeproj
```

## Compatibility

- Android `minSdk`: 26
- Android `targetSdk`: 33
- Desktop: JVM 17

## Legacy Version

The pre-Kotlin-Multiplatform version of the project is available here:

https://github.com/NikolayKuts/Klaf

See [CHANGELOG.md](CHANGELOG.md) for recent implementation changes.

## Animation samples
![name](https://github.com/NikolayKuts/Klaf_kt/blob/develop/preview/data_synchronization_dark_theme.gif)
![name](https://github.com/NikolayKuts/Klaf_kt/blob/develop/preview/autocomplete.gif)
![name](https://github.com/NikolayKuts/Klaf_kt/blob/develop/preview/deck_list_item_animation_dark_them.gif)
![name](https://github.com/NikolayKuts/Klaf_kt/blob/develop/preview/card_transferring_fragment_buttons_animation_dark_theme.gif)
![name](https://github.com/NikolayKuts/Klaf_kt/blob/develop/preview/deck_repetition_screen_animation.gif)
![name](https://github.com/NikolayKuts/Klaf_kt/blob/deck_repetition_screen/preview/smart_text_selection.gif)
![name](https://github.com/NikolayKuts/Klaf_kt/blob/develop/preview/authentication_notifying.gif)

## Screen samples
![name](https://github.com/NikolayKuts/Klaf_kt/blob/develop/preview/deck_list_screen_dark_theme.png)
![name](https://github.com/NikolayKuts/Klaf_kt/blob/develop/preview/deck_list_screen_light_theme.png)

![name](https://github.com/NikolayKuts/Klaf_kt/blob/develop/preview/deck_navigation_dialog_light_theme.png)
![name](https://github.com/NikolayKuts/Klaf_kt/blob/develop/preview/deck_navigation_dialog_dark_theme.png)

![name](https://github.com/NikolayKuts/Klaf_kt/blob/deck_repetition_screen/preview/deck_repetition_screen_dark_theme.JPEG)
![name](https://github.com/NikolayKuts/Klaf_kt/blob/deck_repetition_screen/preview/deck_repetition_screen_light_theme.JPEG)

![name](https://github.com/NikolayKuts/Klaf_kt/blob/develop/preview/card_addition_screen_light_theme.JPEG)
![name](https://github.com/NikolayKuts/Klaf_kt/blob/develop/preview/card_editing_screen_dark_theme.JPEG)

![name](https://github.com/NikolayKuts/Klaf_kt/blob/develop/preview/deck_deleting_dialog_dark_theme.jpg)
![name](https://github.com/NikolayKuts/Klaf_kt/blob/develop/preview/deck_creating_dialog_light_theme.jpg)

![name](https://github.com/NikolayKuts/Klaf_kt/blob/develop/preview/success_mark_indication_light_theme.jpg)
![name](https://github.com/NikolayKuts/Klaf_kt/blob/develop/preview/success_mark_indication_dark_theme.jpg)

![name](https://github.com/NikolayKuts/Klaf_kt/blob/develop/preview/card_transferring_fragment_dark_theme.JPEG)
![name](https://github.com/NikolayKuts/Klaf_kt/blob/develop/preview/deck_choosing_dialog_light_theme.jpg)

