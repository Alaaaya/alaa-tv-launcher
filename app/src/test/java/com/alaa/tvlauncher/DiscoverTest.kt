package com.alaa.tvlauncher

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.input.key.Key
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "en-rUS-w960dp-h540dp-television-xhdpi", application = android.app.Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(ExperimentalTestApi::class)
class DiscoverTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val date=LocalDate.of(2026,10,21)
    private fun home() {
        rule.activity.getSharedPreferences("appearance",0).edit().clear().commit()
        val apps=listOf("Alaa Player","YouTube","Netflix","Shahid","Prime Video").mapIndexed { i,name -> InstalledApp("fixture.$i",name,Intent(),null) }
        rule.mainClock.autoAdvance=false
        rule.setContent {
            LauncherHome(apps,
                PrayerDay(date,listOf("06:15","13:17","15:55","18:29","20:12"),false),
                PrayerDay(date.plusDays(1),listOf("06:17","13:17","15:53","18:27","20:10"),true),
                Weather(14,2,System.currentTimeMillis()),date.atTime(16,0).atZone(berlin),
                listOf(Series(1,"Fixture Drama",null),Series(2,"Night Shift",null),Series(3,"Long Harbor",null),Series(4,"Paper Moon",null)))
        }
        rule.mainClock.advanceTimeBy(2000)
    }
    private fun press(tag:String) {
        rule.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        rule.onNodeWithTag(tag).performKeyInput { keyDown(Key.DirectionCenter); keyUp(Key.DirectionCenter) }
        rule.mainClock.advanceTimeBy(2000)
    }
    private fun capture(name:String) {
        rule.waitForIdle()
        var bitmap: Bitmap?=null
        rule.runOnUiThread {
            val view=rule.activity.window.decorView
            bitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap!!))
        }
        val file=File("build/outputs/screenshots/$name.png")
        file.parentFile!!.mkdirs()
        file.outputStream().use { bitmap!!.compress(Bitmap.CompressFormat.PNG,100,it) }
    }
    @Test fun glassApiGate() {
        assertFalse(glassBlurSupported(22))
        assertFalse(glassBlurSupported(30))
        assertTrue(glassBlurSupported(31))
        assertTrue(glassBlurSupported(36))
    }
    @Test fun solarDayNightTracksSeasonsAndTimezone() {
        assertFalse(isNight(date.atTime(12,0).atZone(berlin)))
        assertTrue(isNight(date.atTime(23,0).atZone(berlin)))
        assertFalse(isNight(LocalDate.of(2026,6,21).atTime(21,0).atZone(berlin)))
        assertTrue(isNight(LocalDate.of(2026,12,21).atTime(18,0).atZone(berlin)))
        val utc=date.atTime(12,0).atZone(berlin).withZoneSameInstant(ZoneOffset.UTC)
        assertEquals(isNight(utc),isNight(utc.withZoneSameInstant(berlin)))
    }
    @Test fun discoverScreenshotDpadDrawerAndTabs() {
        home()
        rule.onNodeWithText("For You").assertExists()
        rule.onNodeWithText("مسجد الرحمة").assertExists()
        rule.onNodeWithTag("prayer-glass").assertExists()
        listOf("06:15","13:17","15:55","18:29","20:12").forEach { time ->
            rule.onAllNodesWithText(time,substring=true).onFirst().assertExists()
        }
        rule.onNodeWithTag("app-fixture.0").assertIsFocused()
        capture("alaa-tv-1.2-light")
        rule.onNodeWithTag("clock-glass").assertExists()
        rule.onNodeWithText("Fixture Drama").assertExists()
        press("all-apps")
        rule.onNodeWithText("Your apps").assertExists()
        rule.onNodeWithText("رجوع").performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        rule.onNodeWithText("رجوع").performKeyInput { keyDown(Key.DirectionCenter); keyUp(Key.DirectionCenter) }
        rule.mainClock.advanceTimeBy(1000)
        rule.onNodeWithText("For You").assertExists()
    }
    @Test fun settingsClassicAndPersistentAppearance() {
        home()
        press("home-settings")
        press("appearance-Dark")
        press("settings-close")
        capture("alaa-tv-1.2-dark")
        press("home-settings")
        press("theme-Classic")
        assertEquals("Classic",rule.activity.getSharedPreferences("appearance",0).getString("theme",""))
        press("settings-close")
        rule.onNodeWithText("مواقيت الصلاة").assertExists()
        rule.onNodeWithText("For You").assertDoesNotExist()
        // Classic's settings button reopens this same theme chooser.
        rule.onNodeWithText("⚙").performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        rule.onNodeWithText("⚙").performKeyInput { keyDown(Key.DirectionCenter); keyUp(Key.DirectionCenter) }
        rule.mainClock.advanceTimeBy(1000)
        press("theme-Discover")
        press("settings-close")
        rule.onNodeWithText("For You").assertExists()
        assertEquals("Dark",rule.activity.getSharedPreferences("appearance",0).getString("appearance",""))
    }
}
