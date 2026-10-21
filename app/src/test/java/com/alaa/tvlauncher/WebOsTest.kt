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
class WebOsTest {
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
                org.json.JSONArray(javaClass.classLoader!!.getResource("tvmaze/shows.json")!!.readText()).let { list ->
                    (0 until list.length()).map { i -> val show=list.getJSONObject(i); Series(show.getInt("id"),show.getString("title"),javaClass.classLoader!!.getResourceAsStream("tvmaze/${show.getInt("id")}.jpg")!!.use { android.graphics.BitmapFactory.decodeStream(it) }) }
                })
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
    @Test fun webOsDefaultScreenshotAndNavigation() {
        home()
        rule.onNodeWithTag("webos-hero").assertExists()
        rule.onNodeWithTag("clock-glass").assertExists()
        rule.onNodeWithTag("prayer-glass").assertExists()
        rule.onNodeWithText("Top picks for you").assertExists()
        rule.onNodeWithText("مواقيت الصلاة · مسجد الرحمة مولهايم").assertExists()
        rule.onNodeWithText("الطقس").assertDoesNotExist()
        rule.onNodeWithTag("hijri-date").assertExists()
        rule.onNodeWithTag("gregorian-date").assertExists()
        rule.onNodeWithTag("ramadan-countdown").assertExists()
        capture("alaa-tv-1.6")
        press("all-apps")
        rule.onNodeWithText("Your apps").assertExists()
        press("drawer-close")
        press("home-settings")
        press("theme-Discover")
        press("settings-close")
        rule.onNodeWithText("For You").assertExists()
        press("home-settings")
        press("theme-webOS")
        press("settings-close")
        rule.onNodeWithTag("webos-hero").assertExists()
        assertEquals("webOS",rule.activity.getSharedPreferences("appearance",0).getString("theme",""))
    }
    @Test fun categoriesResolveInstalledApps() {
        val apps=listOf(InstalledApp("player.alaa","Alaa Player",Intent(),null),InstalledApp("youtube.tv","YouTube",Intent(),null))
        assertEquals("player.alaa",categoryApp(0,apps)?.packageName)
        assertEquals("youtube.tv",categoryApp(3,apps)?.packageName)
        assertNull(categoryApp(0,emptyList()))
    }
}
