# Simple Home: a minimal Android TV launcher

A lightweight, macOS-inspired home screen for Android TV / Google TV. It replaces the stock launcher and has no ads, trailers or tracking. Its only network use is a small weather and air-quality request every 15 minutes, plus an optional pollen request every 6 hours.

## Features
- A macOS-style dock with squircle app icons, magnification and lift, and a collapsible menu (Inputs, Settings, Wallpaper, All apps).
- A Launchpad-style All apps grid.
- Glass widget cards for the clock (with a second time zone), weather (Open-Meteo), air quality (US AQI) and an optional pollen forecast (Google Pollen API).
- A centered quote that rotates every 5 minutes from about 680 sourced public-domain and Wikiquote-verified quotes, with day and evening sets.
- Wallpapers:
  - animated procedural scenes with parallax;
  - your own photos;
  - painted stills, auto-rotating every 30 minutes and matched to day or night by local sunrise and sunset, calculated offline with the NOAA algorithm.
- Festival and US federal holiday banners with countdowns and themed wallpapers.
- Idle mode that fades the UI to show the wallpaper.
- Built with framework Views only (no AndroidX, Compose or Leanback), and runs in about 40–50 MB.

## Build
Requires JDK 17 and the Android SDK (platform 34, build-tools 34.0.0).

```
./gradlew test lint assembleRelease
```

A release build needs your own signing config in `keystore.properties`:
- `storeFile`
- `storePassword`
- `keyAlias`
- `keyPassword`

This file is gitignored.

## Wallpaper content
Photos and paintings are not included in this repo. The app reads them at runtime from its app-specific storage:
- `Android/data/tv.simplehome.launcher/files/photos/` holds the photos plus a `photos.json` manifest.
- `Android/data/tv.simplehome.launcher/files/scenes/` holds the paintings plus a `scenes.json` manifest.

See `PhotoStore.kt` and `StillStore.kt` for the manifest formats. The procedural scenes work without any extra content.

## Pollen (optional)
The pollen line needs your own Google Maps Platform API key with the Pollen API enabled. Restrict the key to the Pollen API and set a daily quota. The key is never built into the APK; write it to the app's storage on the TV:

```
printf %s "$KEY" | adb exec-in "cat > /sdcard/Android/data/tv.simplehome.launcher/files/pollen.key"
adb shell chmod 644 /sdcard/Android/data/tv.simplehome.launcher/files/pollen.key
```

Without the file, the pollen line stays hidden and no pollen request is made.

## Credits
- [Inter](https://rsms.me/inter/) typeface (SIL Open Font License); see `licenses/`.
- Weather and air quality from [Open-Meteo](https://open-meteo.com/).
- ZIP lookup from [Zippopotam.us](https://zippopotam.us/).

## License
The code is released under the [MIT License](LICENSE). The bundled Inter font keeps its own SIL Open Font License (`licenses/Inter-OFL.txt`).
