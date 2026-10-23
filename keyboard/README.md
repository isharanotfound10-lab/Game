# Super Keyboard for Android

Standalone native Android input method under `keyboard/` in the Game repository. It is separate from the Unity game.

## Features implemented in this build
- Native Android IME with QWERTY typing, shift, backspace, space, enter/action-key handling and Android keyboard switcher.
- Optional number row; number/symbol layout; Unicode symbol panel.
- Small offline word-suggestion and common-typo correction engine, plus basic glide typing decoded against a bundled local dictionary. It runs locally and is suppressed for detected password fields.
- Emoji picker with multiple categories.
- Clipboard paste.
- GIF search shortcut that opens Tenor in the browser; GIFs are not inserted directly into the current app.
- Text utilities for uppercase/lowercase/title case/reverse/whitespace cleanup/common typo fixes, date/time, saved snippets, and signature insertion.
- Lightweight offline writing helpers for grammar cleanup, concise phrasing, professional/friendly tone, slight expansion, and sentence case. These are deterministic rules, not generative AI.
- Key-shape personalization with five selectable corner-radius presets.
- Small built-in English, Sinhala, and Tamil word lists, with selectable Sinhala/Tamil keyboard rows.
- Custom three-row keyboard editor: each Unicode code point in the three configured rows becomes a key.
- Session-only clipboard history (items are not persisted after the keyboard service session), sentence auto-capitalization, and optional double-space-to-period punctuation.
- Five themes (Purple, Cyber Pink, Ocean, Emerald, AMOLED), smooth-effects toggle, auto-correct, auto-capitalization, double-space punctuation, optional haptics, saved signature, number-row toggle, QWERTY/Dvorak/Colemak layouts, and compact/normal/tall key sizes.
- GitHub Actions build and unit tests.

## Build
With JDK 17 and Gradle 8.9:

```sh
gradle -p keyboard assembleDebug
gradle -p keyboard testDebugUnitTest
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

## Privacy and limitations
The keyboard does not upload typed text. Suggestions use a small bundled local word list, not a neural model or a large language model. This build does **not** yet include a neural on-device AI model, advanced neural swipe prediction, full downloadable multilingual dictionaries, direct in-app GIF insertion, cloud sync, or a drag-and-drop editor. The English/Sinhala/Tamil word lists are small starter lists, not complete dictionaries. Custom layout editing is a simple three-row text editor, not drag-and-drop. Key appearance customization currently changes corner radius only. The keyboard uses a small bundled English word list and basic deterministic writing transformations. Glide typing is a lightweight dictionary-based implementation and may be less accurate than mature keyboard engines. GIF search opens an external website and sends the search query to that website. Android requires the user to enable and select third-party keyboards in system settings.
