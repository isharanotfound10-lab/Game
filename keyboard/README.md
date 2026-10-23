# Super Keyboard for Android

Standalone native Android input method under `keyboard/` in the Game repository. It is separate from the Unity game.

## Features implemented in this build
- Native Android IME with QWERTY typing, shift, backspace, space, enter/action-key handling and Android keyboard switcher.
- Optional number row; number/symbol layout; Unicode symbol panel.
- Small offline word-suggestion and common-typo correction engine. It runs locally and is suppressed for detected password fields.
- Emoji picker with multiple categories.
- Clipboard paste.
- GIF search shortcut that opens Tenor in the browser; GIFs are not inserted directly into the current app.
- Text utilities for uppercase/lowercase/title case/reverse/whitespace cleanup/common typo fixes, plus date/time and a saved signature.
- Five themes (Purple, Cyber Pink, Ocean, Emerald, AMOLED), smooth-effects toggle, auto-correct toggle, optional haptics, saved signature, number-row toggle, QWERTY/Dvorak/Colemak layouts, and compact/normal/tall key sizes.
- GitHub Actions build and unit tests.

## Build
With JDK 17 and Gradle 8.9:

```sh
gradle -p keyboard assembleDebug
gradle -p keyboard testDebugUnitTest
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

## Privacy and limitations
The keyboard does not upload typed text. Suggestions use a small bundled local word list, not a neural model or a large language model. This build does **not** yet include true glide/swipe typing, an on-device AI model, personalized learned dictionary, full multilingual dictionaries, direct in-app GIF insertion, clipboard history, cloud sync, or a visual custom-layout editor. GIF search opens an external website and sends the search query to that website. Android requires the user to enable and select third-party keyboards in system settings.
