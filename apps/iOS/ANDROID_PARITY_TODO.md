# iOS Android Parity TODO

Last updated: 2026-04-03

## Goal

Reach feature parity on iOS for the platform-specific behavior that is already implemented for Android.

## Current State Summary

- Shared Compose UI and local Room storage already exist and are usable on iOS.
- Most iOS platform services are still placeholders, in-memory implementations, or no-op bindings.
- Because of that, the iOS app currently behaves closer to a local demo shell than to the full Android app.

## Main Gaps

### 1. Real authentication and remote sync

- Replace placeholder iOS auth implementation with a real backend integration.
- Replace local-as-remote bindings with real remote repositories for decks, cards, and storage save versions.
- Implement actual synchronization flow instead of immediately reporting success.
- Add real network reachability checks instead of always returning `true`.

Relevant files:

- `di/src/iosMain/kotlin/com/kuts/klaf/di/DataModule.kt`
- `data/src/iosMain/kotlin/com/kuts/klaf/ios/IosDataPlaceholders.kt`

### 2. Notifications and background work

- Implement local notification scheduling for deck review reminders.
- Implement deck review notification presentation and removal.
- Add notification permission flow on iOS.
- Add background execution support for app reopening and repetition checking if those behaviors are required on iOS as well.
- Handle opening the correct screen after tapping a notification.

Relevant Android reference:

- `data/src/androidMain/kotlin/com/kuts/klaf/common/AndroidAppMaintenanceManager.kt`
- `data/src/androidMain/kotlin/com/kuts/klaf/common/AndroidDeckReviewingReminder.kt`
- `presentation/src/androidMain/kotlin/com/kuts/klaf/common/notifications/AndroidDeckReviewNotifier.kt`

Relevant iOS placeholders:

- `data/src/iosMain/kotlin/com/kuts/klaf/ios/IosDataPlaceholders.kt`
- `presentation/src/iosMain/kotlin/com/kuts/klaf/ios/IosPresentationPlaceholders.kt`

### 3. Embedded web content

- Implement `PlatformWebContentView` for iOS with `WKWebView`.
- Support navigation filtering, page loading callbacks, progress updates, and error reporting.
- Verify the YouGlish flow works from both deck repetition and card editing screens.

Relevant files:

- `presentation/src/iosMain/kotlin/com/kuts/klaf/webContent/PlatformWebContentView.ios.kt`
- `presentation/src/commonMain/kotlin/com/kuts/klaf/webContent/WebContentScreen.kt`

### 4. Audio playback and pronunciation

- Replace the no-op iOS audio manager with a real implementation.
- Support pronunciation preloading and playback.
- Match the lifecycle behavior used by the shared screens.
- Confirm the deck repetition and card management flows react correctly to loading state changes.

Relevant files:

- `data/src/iosMain/kotlin/com/kuts/klaf/ios/IosDataPlaceholders.kt`
- `data/src/androidMain/kotlin/com/kuts/klaf/networking/AndroidCardAudioPlayer.kt`

### 5. Word info, autocomplete, insights, and Cambridge data

- Implement real iOS `IWordInfoRepository`.
- Implement real iOS `IWordAutocompleteRepository`.
- Implement real iOS `IWordMeaningInsightsRepository`.
- Replace the no-op Cambridge provider with a real implementation or intentionally remove that feature on both platforms.
- Verify transcription, native word suggestions, autocomplete, and refreshed insights work end-to-end.

Relevant files:

- `data/src/iosMain/kotlin/com/kuts/klaf/ios/IosDataPlaceholders.kt`
- `presentation/src/iosMain/kotlin/com/kuts/klaf/ios/IosPresentationPlaceholders.kt`
- `presentation/src/commonMain/kotlin/com/kuts/klaf/cardManagement/common/CardManagementViewModel.kt`
- `presentation/src/commonMain/kotlin/com/kuts/klaf/cardManagement/cardEditing/CardEditingViewModel.kt`

### 6. Word insights provider state and Codex session support

- Replace the in-memory provider manager with a persistent implementation.
- If Codex Observer is meant to exist on iOS too, implement the same session lifecycle and error handling.
- Make sure drawer status and provider switching show real state instead of placeholder state.

Relevant files:

- `data/src/iosMain/kotlin/com/kuts/klaf/ios/IosDataPlaceholders.kt`
- `data/src/commonMain/kotlin/com/kuts/klaf/networking/wordInsights/WordInsightsProviderManager.kt`

### 7. Process text, launch routing, deep links, and notification routing

- Implement an iOS equivalent for incoming selected text if this feature is needed on iOS.
- Add launch request handling for notification taps and other external entry points.
- Make sure the iOS root can route directly to deck list, interim card addition, or deck repetition when opened externally.

Relevant files:

- `presentation/src/iosMain/kotlin/com/kuts/klaf/common/externalActions/IosExternalAppActions.kt`
- `presentation/src/iosMain/kotlin/com/kuts/klaf/navigation/IosKlafNavHost.kt`
- `apps/iOS/iosApp/ContentView.swift`

### 8. Persistence and state restoration parity

- Replace in-memory deck repetition info storage with persistent storage.
- Decide whether review state should survive view recreation, app backgrounding, or full restart on iOS.
- If parity is required, introduce an iOS state store strategy comparable to the Android saved-state approach.

Relevant files:

- `di/src/iosMain/kotlin/com/kuts/klaf/di/IosDeckReviewStateStoreFactory.kt`
- `presentation/src/iosMain/kotlin/com/kuts/klaf/ios/IosPresentationPlaceholders.kt`
- `data/src/iosMain/kotlin/com/kuts/klaf/ios/IosDataPlaceholders.kt`

Android reference:

- `di/src/androidMain/kotlin/com/kuts/klaf/di/AndroidDeckReviewStateStoreFactory.kt`
- `data/src/commonMain/kotlin/com/kuts/klaf/dataStore/implementations/DataStoreDeckRepetitionInfoRepository.kt`

### 9. Crash reporting

- Replace the no-op iOS crashlytics binding with a real reporting implementation.
- Verify shared view models and flows still report failures consistently on iOS.

Relevant files:

- `data/src/iosMain/kotlin/com/kuts/klaf/ios/IosDataPlaceholders.kt`

### 10. Old app data transfer

- Decide whether legacy data import is needed on iOS.
- If yes, replace the no-op old app transfer repository with a real migration path.

Relevant files:

- `data/src/iosMain/kotlin/com/kuts/klaf/ios/IosDataPlaceholders.kt`

## Recommended Implementation Order

1. Replace placeholder DI bindings with real iOS implementations for auth, remote repositories, and synchronization.
2. Implement notifications, permission flow, and notification-driven app routing.
3. Implement audio playback and `WKWebView`.
4. Implement word info, autocomplete, insights, and Cambridge data.
5. Implement persistence and state restoration parity.
6. Add crash reporting.
7. Add optional old-app migration support if still needed.

## Definition Of Done

- No critical iOS feature path depends on `NoOp`, `TODO`, or in-memory-only placeholder behavior.
- The main Android user journeys are reproducible on iOS:
  - authentication
  - data synchronization
  - card addition and editing
  - pronunciation playback
  - deck repetition
  - reminder scheduling and notification handling
  - embedded web content
  - word insights provider selection
