package com.alaa.tvlauncher

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.test.core.app.ApplicationProvider

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LiteBitmapsTest {
    @Test fun largeAssetsAreSampled() {
        assertEquals(4, LiteBitmaps.sample(7680, 4320, 960, 540))
        assertEquals(1, LiteBitmaps.sample(240, 360, 240, 360))
    }
    @Test fun wallpaperIsBoundedAndCached() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val image = LiteBitmaps.resource(ctx.resources, R.drawable.prayer_wallpaper, 960, 540)!!
        assertTrue(image.width <= 960)
        assertTrue(image.height <= 540)
        assertSame(image, LiteBitmaps.resource(ctx.resources, R.drawable.prayer_wallpaper, 960, 540))
    }
    @Test fun liteDisablesBlurOnEverySdk() {
        (22..36).forEach { assertFalse(glassBlurSupported(it)) }
    }
}
