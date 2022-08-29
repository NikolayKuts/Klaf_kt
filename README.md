# Klaf

## Discription

The application is designed to help you memorize foreign words using the mnemonic method. The method is based on working with decks of cards and repeating them at a certain interval.
The application calculates the time until the next repetition and sends a notification to repeat the deck. If each subsequent repetition takes the same amount of time as the previous one, or less, the time interval until the next repetition will be increased. If the repetition takes longer, then the interval until the next reminder will be reduced.
The application allows you to create cards with the automatic creation of a transcription template. And also it is possible to listen to the pronunciation of English words.


## Technology stack
* The code is written with _**Kotlin**_
*  _**MVVM**_ + _**Clean Architecture**_
* Access to storage via _**Room database**_
* For asynchronous operations used _**Coroutines**_ and _**Coroutine flows**_
* For Ui navigation used _**Navigation component**_
* For dependency injection used _**Hilt**_
* For Ui used _**Jetpack Compose**_ and _**XML**_
* For scheduling work used _**Work Manager**_
* For uploading and playing the pronunciation of words used _**Media Player**_

## Features
* Single Activity pattern
* Navigation to the app from Smart Text Seletion Menu
* Ability to listen to the pronunciation of English words
* Deck repeat alert by notifications
* Creation of transcriptions
* Automatic creation of a transcription template
* _**Theme**_: dark and light

## Compatibility
From API level 21 to 32

### Old version
https://github.com/NikolayKuts/Klaf

## Screen samples
![name](https://github.com/NikolayKuts/Klaf_kt/blob/develop/preview/deck_list_screen_light_theme.png)
![name](https://github.com/NikolayKuts/Klaf_kt/blob/develop/preview/deck_list_screen_dark_theme.png)

![name](https://github.com/NikolayKuts/Klaf_kt/blob/develop/preview/deck_navigation_dialog_light_theme.png)
![name](https://github.com/NikolayKuts/Klaf_kt/blob/develop/preview/deck_navigation_dialog_dark_theme.png)

![name](https://github.com/NikolayKuts/Klaf_kt/blob/develop/preview/deck_repetition_screen_dark_theme.png)
![name](https://github.com/NikolayKuts/Klaf_kt/blob/develop/preview/deck_repetition_screen_light_theme.png)

![name](https://github.com/NikolayKuts/Klaf_kt/blob/develop/preview/card_addition_screen_light_theme.png)
![name](https://github.com/NikolayKuts/Klaf_kt/blob/develop/preview/card_editing_screen_dark_theme.png)

![name](https://github.com/NikolayKuts/Klaf_kt/blob/develop/preview/deck_deleting_dialog_dark_theme.png)
![name](https://github.com/NikolayKuts/Klaf_kt/blob/develop/preview/deck_creating_dialog_light_theme.png)
