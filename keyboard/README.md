# Super Keyboard for Android

Standalone native Android keyboard project stored in the `keyboard/` folder of the Game repository. It does not replace or modify the Unity game project.

## Current MVP
- Native Android Input Method Service
- Purple dark/glass-inspired keys
- QWERTY and basic symbol layouts
- Shift, delete, space, enter, language picker
- Clipboard paste button and quick emoji insertion
- Android settings links to enable and select the keyboard
- GitHub Actions build and debug APK artifact

## Build
From this directory, with JDK 17 and Gradle 8.9 installed:

```sh
gradle assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Important
Android requires the user to enable and select a third-party keyboard in system settings. This project does not secretly capture keystrokes or transmit typed text. AI, advanced prediction, theme editor, and custom layouts are not implemented yet.
