# Alaa TV

A personal native Android TV home screen, inspired by the owner's approved flat webOS-style reference. Kotlin, Compose for TV. Android 5.1/API 22+.

The installed-app shelf prioritizes Alaa Player, YouTube, Netflix, Shahid, Prime Video and browsers when installed. It does not pretend missing apps are installed. All apps includes both leanback and regular launchable apps, with local search. Settings opens Android settings. The remote's Back button closes the app drawer rather than exiting Home.

Prayer times are from https://mawaqit.net/de/arrahma-mulheim (Arrahma Moschee e.V., Mülheim). The app parses configuration JSON without executing website scripts. The daily five-time array takes priority for the fetched date. Other days use the yearly calendar, excluding sunrise. Cache is retained offline and labeled; a previous year's calendar is not reused. Countdown uses Europe/Berlin, including DST and tomorrow's Fajr. No adhan audio in this version.

Weather is from Open-Meteo, CC BY 4.0 (https://open-meteo.com/), for the mosque/Mülheim coordinates. Refreshed every 15 minutes while the launcher is visible, cached with a stale label. Personal non-commercial use only.

Clock and prayer countdown update live. Requests refresh on return to Home, and are suspended in the background. Artwork is a bundled generated alpine landscape with rotating captions, not commercial streaming promotions. No accounts, tracking, API secrets, or privileged device changes.

## Build

Use Java 17 and Android SDK 36. Create local.properties with your sdk.dir, then run:

```
./gradlew :app:assembleDebug :app:testDebugUnitTest --no-daemon
```

APK is app/build/outputs/apk/debug/app-debug.apk. The package is com.alaa.tvlauncher, version 1.1.

## Install and choose Home

Install the APK using your TV's normal sideload flow. The app appears under Alaa TV. Choose it under default Home apps if the TV supports this. Some manufacturers block third-party Home selection. Installing does not disable/remove the stock launcher and does not debloat the TV. Keep the stock launcher available for recovery. Do not disable system packages to force this app.

The debug APK is intended for this personal TV, not a store production release. A release signing key and real-device performance validation are not yet provided.

## Validation

Robolectric native-graphics UI capture uses clearly labeled fixture installed apps, sample weather, and mosque fixture timings. This is not a screenshot of the owner's television. Unit tests cover prayer extraction, sunrise exclusion, string-safe JSON extraction, DST, overnight rollover and basic drawer navigation.

## Known limitations

No physical TV installation or remote-control test yet. The hero currently uses one bundled landscape and three rotating captions. No HDMI/tuner integration. App icon appearance depends on the actual installed packages. Manufacturer-specific Home selection cannot be guaranteed. Mosque page structure may change and require a parser update.


## v1.1 Discover theme

The default home now follows the owner's Free My TV reference: a soft off-white day palette, charcoal night palette, Explore / Movies / Shows tabs, a wide landscape card beside two portrait art cards, a compact live prayer strip, small weather/clock and a wide app-banner dock. Settings offers **Discover** and the original **Classic** theme. Both theme and Discover appearance (Auto, Light, Dark) persist locally. Auto uses approximate NOAA solar elevation at the Mülheim mosque coordinates and works offline. No blur is used.

Artwork is explicitly labeled **sample art**. Tabs change bundled artwork/title collections, not real film/series catalogs. All three artwork cards open the app drawer, not a fictional video or unavailable stream. There is no TMDB key and no live recommendation service. The two portrait illustrations are drawn locally and the landscape is the original bundled art.

The dock loads actual installed TV activity/application banners, with installed icons as fallback, and text only if neither can be loaded. Only installed launchable apps are shown; Alaa Player is first when installed. Screenshot tests use fixture app names without installed binaries, so their tiles are text, not evidence of the owner's installed apps. The live MAWAQIT repository/countdown and weather cache behavior are unchanged. Football widgets and their network feed have been removed entirely from both themes.

Design inspiration: [Free My TV](https://github.com/simbasjungles/free-my-tv), MIT, reviewed HomeView.java's title/tab/card/shelf composition. This release uses an original Compose implementation, does not copy the upstream Canvas renderer or include its ADB/debloat/privileged package-disabling code.

Seven automated Robolectric tests cover prayer parsing/DST, Classic drawer remote navigation, Discover screenshots, tab selection, D-pad drawer opening/closing, switching Classic and back from Settings, saved appearance, and solar day/night across seasons and zones. Screenshots are rendered Android views at 1920x1080 using fixture prayers/weather/apps. Physical-TV performance and manufacturer Home selection remain untested. Debug signing matches v1.0 for in-place updates on the same installation.
