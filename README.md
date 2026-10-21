# Alaa TV

A personal native Android TV home screen, inspired by the owner's approved flat webOS-style reference. Kotlin, Compose for TV. Android 5.1/API 22+.

The installed-app shelf prioritizes Alaa Player, YouTube, Netflix, Shahid, Prime Video and browsers when installed. It does not pretend missing apps are installed. All apps includes both leanback and regular launchable apps, with local search. Settings opens Android settings. The remote's Back button closes the app drawer rather than exiting Home.

Prayer times are from https://mawaqit.net/de/arrahma-mulheim (Arrahma Moschee e.V., Mülheim). The app parses configuration JSON without executing website scripts. The daily five-time array takes priority for the fetched date. Other days use the yearly calendar, excluding sunrise. Cache is retained offline and labeled; a previous year's calendar is not reused. Countdown uses Europe/Berlin, including DST and tomorrow's Fajr. No adhan audio in this version.

Weather is from Open-Meteo, CC BY 4.0 (https://open-meteo.com/), for the mosque/Mülheim coordinates. Refreshed every 15 minutes while the launcher is visible, cached with a stale label. Personal non-commercial use only.

The football card uses TheSportsDB's public demonstration endpoint, limited to a few matches. It is not a complete match schedule or guaranteed live-score feed. Hidden if there is no available match today. Kickoff is converted from UTC to Europe/Berlin.

Clock and prayer countdown update live. Requests refresh on return to Home, and are suspended in the background. Artwork is a bundled generated alpine landscape with rotating captions, not commercial streaming promotions. No accounts, tracking, API secrets, or privileged device changes.

## Build

Use Java 17 and Android SDK 36. Create local.properties with your sdk.dir, then run:

```
./gradlew :app:assembleDebug :app:testDebugUnitTest --no-daemon
```

APK is app/build/outputs/apk/debug/app-debug.apk. The package is com.alaa.tvlauncher, version 1.0.

## Install and choose Home

Install the APK using your TV's normal sideload flow. The app appears under Alaa TV. Choose it under default Home apps if the TV supports this. Some manufacturers block third-party Home selection. Installing does not disable/remove the stock launcher and does not debloat the TV. Keep the stock launcher available for recovery. Do not disable system packages to force this app.

The debug APK is intended for this personal TV, not a store production release. A release signing key and real-device performance validation are not yet provided.

## Validation

Robolectric native-graphics UI capture uses clearly labeled fixture installed apps, sample football and weather, and mosque fixture timings. This is not a screenshot of the owner's television. Unit tests cover prayer extraction, sunrise exclusion, string-safe JSON extraction, DST, overnight rollover and basic drawer navigation.

## Known limitations

No physical TV installation or remote-control test yet. The hero currently uses one bundled landscape and three rotating captions. No HDMI/tuner integration. App icon appearance depends on the actual installed packages. Manufacturer-specific Home selection cannot be guaranteed. Match coverage depends on the public API and can be absent. Mosque page structure may change and require a parser update.
