# Copify

An Android app that adds a **Copy to clipboard** target to the system share menu. Share a screenshot, one or more images, or text from any app, and Copify copies it to the clipboard and closes.

## How it works

- `ShareToClipboardActivity` is an invisible share target for `image/*` (single or multiple) and `text/*`.
- Shared images are copied into the app's cache and exposed through a `FileProvider`, so the app you paste into can read them after the original share grant expires.
- `MainActivity` is a Compose launcher screen with usage instructions and a "Try it now" button.

## Building

Requires JDK 17 and the Android SDK (min SDK 26, target SDK 35).

```sh
./gradlew assembleDebug        # build a debug APK
./gradlew installDebug         # install on a connected device
```

Android Studio creates `local.properties` with your SDK path; it is not checked in.
