Alaa TV 1.9 Lite
===============

Same webOS home geometry and separate mosque prayer page as 1.8. Package remains
com.alaa.tvlauncher, versionCode 20, versionName 1.9 Lite. Older published APKs
are unchanged; Classic and Discover remain selectable in this build.

Lite removes wallpaper blur, frosted sheen, focus glow, hero rotation and the
Classic hero transition. Focus retains a small TV Material scale and white border.
Prayer cards use flat translucent colors. MAWAQIT and clock/countdown remain live.
Unused weather polling is stopped.

Bundled photos decode on Dispatchers.IO, capped at 960x540 in RGB_565 with a
4 MiB resource LRU. Daily series are bounded to five, poster decoding to 240x360
and backdrop decoding to 960x540; duplicate fallback decoding is avoided.
App icons remain alpha-capable; package discovery/icon rendering runs on IO.
R8 and resource shrinking are enabled for release. Existing hand-authored
baseline-profile rules are extended to webOS and prayer screens, not represented
as device-recorded profiles.

Validation covers Robolectric unit/Compose navigation tests, bounded resource
decoding/cache tests and release compilation. Real-box frame-time and memory
improvements require installing and profiling this release on the actual device.
