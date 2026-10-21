package com.alaa.tvlauncher

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.input.key.Key
import androidx.test.core.app.ApplicationProvider
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
class LauncherTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private fun configuration() = PrayerParser.extract(javaClass.classLoader!!.getResource("mawaqit.html")!!.readText())
    @Test fun prayerExtractionAndSunriseExclusion() {
        val c = configuration()
        val date = LocalDate.of(2026,10,21)
        val today = PrayerParser.day(c,date,date,false)!!
        assertEquals(listOf("06:15","13:17","15:55","18:29","20:12"),today.times)
        val tomorrow = PrayerParser.day(c,date.plusDays(1),date,true)!!
        val raw = c.getJSONArray("calendar").getJSONObject(9).getJSONArray("22")
        assertEquals(listOf(0,2,3,4,5).map { raw.getString(it) },tomorrow.times)
        assertEquals(5,tomorrow.times.size)
    }
    @Test fun nextPrayerAfterMidnightAndDst() {
        val date = LocalDate.of(2026,10,24)
        val a = PrayerDay(date, listOf("06:20","13:15","15:50","18:20","20:00"),false)
        val b = PrayerDay(date.plusDays(1), listOf("05:20","12:15","14:50","17:20","19:00"),true)
        val next = PrayerParser.next(a,b,date.atTime(23,50).atZone(berlin))!!
        assertEquals(0,next.first)
        assertEquals(b.date,next.second.toLocalDate())
        assertEquals(ZoneOffset.ofHours(1),next.second.offset)
        assertEquals(390,Duration.between(date.atTime(23,50).atZone(berlin),next.second).toMinutes())
    }
    @Test fun parserIgnoresBracesInStrings() {
        assertEquals("hello } {",PrayerParser.extract("""<script>var confData = {"message":"hello } {","times":[]};</script>""").getString("message"))
    }
    @Test fun screenshotAndNavigation() {
        val date = LocalDate.of(2026,10,21)
        val apps = listOf("Alaa Player","YouTube","Netflix","Shahid","Prime Video","المتصفح").mapIndexed { i, label ->
            InstalledApp("fixture.$i",label,Intent(),null)
        }
        rule.mainClock.autoAdvance = false
        rule.setContent {
            ClassicHome(apps,
                PrayerDay(date,listOf("06:15","13:17","15:55","18:29","20:12"),false),
                PrayerDay(date.plusDays(1),listOf("06:17","13:17","15:53","18:27","20:10"),true),
                Weather(14,2,System.currentTimeMillis()),
                date.atTime(17,56).atZone(berlin))
        }
        rule.mainClock.advanceTimeBy(1000)
        rule.waitForIdle()
        rule.onNodeWithText("Alaa TV").assertExists()
        rule.onNodeWithText("مواقيت الصلاة").assertExists()
        rule.onNodeWithText("18:29").assertExists()
        var bitmap: Bitmap? = null
        rule.runOnUiThread {
            val view = rule.activity.window.decorView
            bitmap = Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap!!))
        }
        val output = File("build/outputs/screenshots/alaa-tv-home.png")
        output.parentFile!!.mkdirs()
        output.outputStream().use { bitmap!!.compress(Bitmap.CompressFormat.PNG,100,it) }
        rule.onNodeWithTag("all-apps").performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        rule.onNodeWithTag("all-apps").performKeyInput { keyDown(Key.DirectionCenter); keyUp(Key.DirectionCenter) }
        rule.mainClock.advanceTimeBy(300)
        println(rule.onRoot().printToString())
        rule.onNodeWithText("رجوع").assertExists()
        rule.onNodeWithText("مواقيت الصلاة").assertDoesNotExist()
        rule.onNodeWithText("الطقس").assertDoesNotExist()
        rule.onNodeWithText("رجوع").performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        rule.onNodeWithText("رجوع").performKeyInput { keyDown(Key.DirectionCenter); keyUp(Key.DirectionCenter) }
        rule.mainClock.advanceTimeBy(300)
        rule.onNodeWithText("مواقيت الصلاة").assertExists()
    }
}
