<p align="center">
  <img src="https://github.com/NikolayKuts/Klaf_kt/blob/develop/preview/Klaf_icon_128px.png?raw=true" alt="Sublime's custom image"/>
</p>

# Klaf

## Discription

The application is designed to help you memorize foreign words using the mnemonic method. The method is based on working with decks of cards and repeating them at a certain interval.
The application calculates the time until the next repetition and sends a notification to repeat the deck. If each subsequent repetition takes the same amount of time as the previous one, or less, the time interval until the next repetition will be increased. If the repetition takes longer, then the interval until the next reminder will be reduced.
The application allows you to create cards with the automatic creation of a transcription template. And also it is possible to listen to the pronunciation of English words.


## Technology stack
* The code is written with [_**Kotlin**_](https://kotlinlang.org/)
* [_**MVVM**_](https://en.wikipedia.org/wiki/Model%E2%80%93view%E2%80%93viewmodel) + [_**Clean Architecture**_](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
* Access to local storage via [_**Room database**_](https://developer.android.com/jetpack/androidx/releases/room?gclid=Cj0KCQiA4aacBhCUARIsAI55maHsI2AXFdILFEuxiZANnj4osoCdiKzs8wbbReVJ94HUD4Mo_CS3k-UaAlj1EALw_wcB&gclsrc=aw.ds), [_**DataStore**_](https://developer.android.com/topic/libraries/architecture/datastore?gclid=Cj0KCQiA4aacBhCUARIsAI55maF8MzhHpnejUNKEjuWnHm3UNt1YOdiCIfE2Xe_yn37gLw7Ap5rV5r0aAjLfEALw_wcB&gclsrc=aw.ds)
* Access to remote storage via [_**Firebase**_](https://firebase.google.com/)
* For asynchronous operations used [_**Coroutines**_](https://kotlinlang.org/docs/coroutines-overview.html) and [_**Coroutine flows**_](https://developer.android.com/kotlin/flow)
* For Ui navigation used [_**Navigation component**_](https://developer.android.com/guide/navigation?gclid=Cj0KCQiA4aacBhCUARIsAI55maG6BEZpROClIXY-7nAHZaGsZe5It8jIBKkVyNfObruJf3uzhwVOVTwaAhXsEALw_wcB&gclsrc=aw.ds)
* For dependency injection used [_**Hilt**_](https://dagger.dev/hilt/)
* For Ui used [_**Jetpack Compose**_](https://developer.android.com/jetpack/compose?gclid=Cj0KCQiA4aacBhCUARIsAI55maGeOQkxRqFdEewf0v20hNqbvNWxj42X_bppURJRlGg6UtpjDgiM0JgaAoiVEALw_wcB&gclsrc=aw.ds) and [_**XML**_](https://developer.android.com/develop/ui/views/layout/declaring-layout)
* For scheduling work used [_**Work Manager**_](https://developer.android.com/topic/libraries/architecture/workmanager?gclid=Cj0KCQiA4aacBhCUARIsAI55maFaZUX1X7MJBVufx-d4U0v-21CXkeivW3igzDQe5cXozmLN4wKd60MaAh_QEALw_wcB&gclsrc=aw.ds)
* For testing used [_**MockK**_](https://mockk.io/) and [_**JUnit4**_](https://junit.org/junit4/)
* For uploading and playing the pronunciation of words used [_**Media Player**_](https://developer.android.com/reference/android/media/MediaPlayer)

## Features
* Single Activity pattern
* Navigation to the app from [Smart Text Seletion Menu](https://developer.android.com/about/versions/marshmallow/android-6.0-changes.html?hl=ru#behavior-text-selection)
* Ability to listen to the pronunciation of English words
* Deck repeat alert by notifications
* Ability to save data on remote storage
* Animated UI
* Creation of transcriptions
* Automatic creation of a transcription template
* _**Theme**_: dark and light

## Compatibility
From API level 21 to 32

### Old version
https://github.com/NikolayKuts/Klaf

## Animation samples
![name](https://github.com/NikolayKuts/Klaf_kt/blob/develop/preview/deck_list_screen_animation.gif)
![name](https://github.com/NikolayKuts/Klaf_kt/blob/develop/preview/deck_repetition_screen_animation.gif)
![name](https://github.com/NikolayKuts/Klaf_kt/blob/develop/preview/deck_list_item_animation_dark_them.gif)
![name](https://github.com/NikolayKuts/Klaf_kt/blob/develop/preview/card_transferring_fragment_buttons_animation_dark_theme.gif)

## Screen samples
![name](https://github.com/NikolayKuts/Klaf_kt/blob/develop/preview/deck_list_screen_dark_theme.png)
![name](https://github.com/NikolayKuts/Klaf_kt/blob/develop/preview/deck_list_screen_light_theme.png)

![name](https://github.com/NikolayKuts/Klaf_kt/blob/develop/preview/deck_navigation_dialog_light_theme.png)
![name](https://github.com/NikolayKuts/Klaf_kt/blob/develop/preview/deck_navigation_dialog_dark_theme.png)

![name](https://github.com/NikolayKuts/Klaf_kt/blob/develop/preview/deck_repetition_screen_dark_theme.png)
![name](https://github.com/NikolayKuts/Klaf_kt/blob/develop/preview/deck_repetition_screen_light_theme.png)

![name](https://github.com/NikolayKuts/Klaf_kt/blob/develop/preview/card_addition_screen_light_theme.png)
![name](https://github.com/NikolayKuts/Klaf_kt/blob/develop/preview/card_editing_screen_dark_theme.png)

![name](https://github.com/NikolayKuts/Klaf_kt/blob/develop/preview/deck_deleting_dialog_dark_theme.png)
![name](https://github.com/NikolayKuts/Klaf_kt/blob/develop/preview/deck_creating_dialog_light_theme.png)

![name](https://github.com/NikolayKuts/Klaf_kt/blob/develop/preview/success_mark_indication_light_theme.jpg)
![name](https://github.com/NikolayKuts/Klaf_kt/blob/develop/preview/success_mark_indication_dark_theme.jpg)

![name](https://github.com/NikolayKuts/Klaf_kt/blob/develop/preview/card_transferring_fragment_dark_theme.JPEG)
![name](https://github.com/NikolayKuts/Klaf_kt/blob/develop/preview/deck_choosing_dialog_light_theme.JPEG)


