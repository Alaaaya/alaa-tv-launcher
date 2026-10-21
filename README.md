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

### tv-makeover source adaptation

Discover now also adapts the actual paisanos-io/tv-makeover DreamOS source geometry: 75dp horizontal inset, 24sp Inter Tight wordmark with 18sp clock beneath, 42dp header controls, 174.5×82.5dp app tiles separated by 24dp, and a rounded translucent dock with a 1dp white edge. The existing bundled landscape supplies a wallpaper, with light/dark translucent overlays following the saved solar setting. The owner's requested poster row and MAWAQIT strip are retained above the dock. These additions make this a hybrid, not a pixel-identical DreamOS clone. Glass is translucent compositing, not dynamic backdrop blur.

Source inspected at https://github.com/paisanos-io/tv-makeover, templates/dreamos. MIT notice, including cleanyourtv attribution, is packaged in assets/tv-makeover-LICENSE.txt; the reused Inter Tight font and OFL notice are bundled. DreamOS wallpaper and sounds are explicitly owner's artwork and were NOT redistributed. No upstream debloat scripts or privileges are included. The upstream template does not itself provide movie posters or an automatic day/night system, so our existing sample posters and solar switching remain.

### Frosted glass and performance pass

Release builds now enable R8 and resource shrinking and include ProfileInstaller 1.4.1 plus conservative hand-authored baseline rules for entry/UI paths, merged with the dependency profiles. These rules have NOT been collected from a TV journey or benchmarked on a device. Release signing intentionally retains the existing installation certificate.

API 31+ glass layers use a blurred static bundled landscape beneath a translucent gradient, while all foreground text, logos and focus outlines stay sharp. Older devices use the translucent gradient only. This is a local frosted image layer, NOT live sampling of arbitrary views behind each card. The static scene keeps GPU work bounded; no continuously moving full-screen shader or wallpaper animation. TV Material focus transforms and lazy dock scrolling remain hardware-layer based. Dock items now have stable package keys and reusable content types. Day/night color animation reads occur in draw instead of rebuilding the tree every frame.

The root clock updates once a minute instead of invalidating the entire home every second. Discover's second countdown is isolated inside the prayer-strip composition and suspends when the activity is not STARTED. Image loading and network requests remain off the main thread.

60fps is a target, not an achieved measurement. No physical TV, GPU framestats, Macrobenchmark, or device-generated baseline profile was available here. Before claiming zero jank, measure the signed release on the owner's TV across cold start, repeated left/right focus, drawer scrolling and theme changes; record frame-time p50/p90/p99 and frames over 16.7ms.


### Glass TV Home reference review

Inspected https://github.com/moondayli/glass-tv-home, including LICENSE,
Glass.java and Backdrop.java. Code is MIT, copyright 2026 Glass TV Home
contributors; its full notice is packaged as assets/GLASS_TV_HOME_LICENSE.txt.
Adapted the separate top-sheen and diagonal bright/dim/bright rim treatment
to Compose. The overlay is drawn AFTER card artwork so opaque app banners
and posters retain a glass highlight. Brushes are remembered; no pointer
handlers are added. Layout remains the tv-makeover hybrid, Classic remains.

The reference's 96x54 CPU-blurred, screen-coordinate BitmapShader sampling
was reviewed but not copied: our current glass still uses local static-art
RenderEffect blur on API31+, gradient fallback below. This is NOT live
backdrop sampling or the reference's pixel-identical glass. No adware,
ADB, TV-disabling or installation scripts were run. No TVmaze/Unsplash
artwork or their separate licenses were imported.

### v1.2 widgets and series

Discover now has exactly two widget cards: a large Arabic date/clock and
a 104dp frosted prayer card with all five times, next-prayer highlight and
a live countdown. Discover no longer displays weather. Classic remains available.

The poster row loads real posters for English scripted series airing today,
ranked by TVmaze weight using its keyless web and US schedules. This is
TVmaze popularity, not verified streaming-viewership rankings. Metadata and
poster files are retained offline; refresh is attempted every six hours and
a successful day's response is reused until the next date. No API key or
TMDB configuration is needed. TVmaze show data is CC BY-SA and attributed in
settings. Posters are externally supplied promotional artwork, not app logos.
Poster cards open the app drawer, not an episode stream.

Robolectric screenshots use explicitly named fixture shows without remote
posters so UI tests remain deterministic and never misrepresent a live feed.

## v1.3 webOS-inspired home

New default `webOS` layout follows the supplied 24/25 home reference, without
using LG branding or proprietary artwork. Discover and Classic remain selectable.
The left rail provides profile/launcher settings, a local notification status,
Android voice search, and settings. Five colored shortcuts resolve installed apps
by label/package (or open the app drawer when unavailable); they do not deep-link
into private movie/sports sections of third-party apps. Series cards open a viewing
app, not guaranteed playback of the pictured title. APPS opens a vertical app list.
TVmaze supplies daily English scripted picks ranked by weight, not viewership.
Landscape background art is preferred when available and cached with poster
fallbacks. Offline cached series and MAWAQIT calendar remain available.
Only clock/date and MAWAQIT prayer widgets are shown. Focus uses TV Material scale
and white outlines. 60fps is a target, not a measured guarantee on physical TV.
Robolectric screenshots use genuine downloaded TVmaze images and fixture app names
(no claim that those apps are actually installed on a TV). Test fixtures record
source URLs in `app/src/test/resources/tvmaze/shows.json` (TVmaze CC BY-SA).
