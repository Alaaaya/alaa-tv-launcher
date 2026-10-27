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
@Config(sdk=[34],qualifiers="ar-rSA-w960dp-h540dp-television-xhdpi",application=android.app.Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(ExperimentalTestApi::class)
class LookTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private fun press(tag:String) {
        runCatching { rule.onNodeWithTag(tag).performScrollTo() }.onFailure { rule.onNodeWithTag(tag).assertExists() }
        rule.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        rule.onNodeWithTag(tag).performKeyInput { keyDown(Key.DirectionCenter); keyUp(Key.DirectionCenter) }
        rule.mainClock.advanceTimeBy(2000)
    }
    private fun capture(name:String) {
        rule.waitForIdle()
        rule.runOnUiThread {
            val v=rule.activity.window.decorView
            val b=Bitmap.createBitmap(v.width,v.height,Bitmap.Config.ARGB_8888)
            v.draw(Canvas(b))
            val f=File("build/outputs/screenshots/$name.png"); f.parentFile!!.mkdirs()
            f.outputStream().use { b.compress(Bitmap.CompressFormat.PNG,100,it) }; b.recycle()
        }
    }
    @Test fun defaultsPersistenceRemoteAndStyles() {
        Thread { Thread.sleep(150000); Thread.getAllStackTraces().forEach { (th,st)-> if(th.name.contains("main",true)||th.name.contains("Test",true)) { System.err.println("DUMP "+th.name); st.take(40).forEach{ System.err.println("DUMP   at "+it) } } }; System.err.flush() }.apply{isDaemon=true}.start()
        val prefs=rule.activity.getSharedPreferences("appearance",0)
        prefs.edit().clear().commit()
        assertEquals(LookOptions(),LookOptions.read(prefs))
        val apps=(1..12).map { InstalledApp("fixture.$it","App $it",Intent(),null) }
        rule.mainClock.autoAdvance=false
        rule.setContent { LauncherHome(apps,null,null,null,LocalDate.of(2026,10,27).atTime(20,0).atZone(berlin)) }
        rule.mainClock.advanceTimeBy(2000)
        System.err.println("STEP 1"); System.err.flush()
        capture("look20-original")
        System.err.println("STEP 2"); System.err.flush()
        press("home-settings")
        rule.mainClock.advanceTimeBy(1000)
        System.err.println("STEP 3"); System.err.flush()
        press("open-look")
        rule.onNodeWithTag("look-page").assertExists()
        System.err.println("STEP 4"); System.err.flush()
        capture("look20-settings")
        System.err.println("STEP 5"); System.err.flush()
        press("style-cinema"); press("wallpaper-dusk"); press("columns-6"); press("focus-bold")
        assertEquals(LookOptions("dusk","cinema",6,"bold"),LookOptions.read(prefs))
        System.err.println("STEP 6"); System.err.flush()
        press("look-close"); press("settings-close")
        System.err.println("STEP 7"); System.err.flush()
        capture("look20-cinema")
        System.err.println("STEP 8"); System.err.flush()
        press("all-apps"); rule.mainClock.advanceTimeBy(1000)
        rule.onNodeWithTag("drawer-fixture.1").assertExists()
        System.err.println("STEP 9"); System.err.flush()
        capture("look20-apps-six")
        System.err.println("STEP 10"); System.err.flush()
        press("drawer-close"); rule.mainClock.advanceTimeBy(1000)
        System.err.println("STEP 11"); System.err.flush()
        press("home-settings"); rule.mainClock.advanceTimeBy(1000)
        System.err.println("STEP 12"); System.err.flush()
        press("open-look"); press("style-ocean"); press("wallpaper-forest"); press("focus-fill")
        System.err.println("STEP 13"); System.err.flush()
        press("look-close"); press("settings-close")
        System.err.println("STEP 14"); System.err.flush()
        capture("look20-ocean")
        System.err.println("STEP 15"); System.err.flush()
        press("home-settings"); rule.mainClock.advanceTimeBy(1000)
        System.err.println("STEP 16"); System.err.flush()
        press("open-look"); press("look-reset")
        assertEquals(LookOptions(),LookOptions.read(prefs))
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.mainClock.advanceTimeBy(1000)
        rule.onNodeWithTag("settings-close").assertExists()
        System.err.println("STEP 17"); System.err.flush()
        press("settings-close")
        System.err.println("STEP 18"); System.err.flush()
        capture("look20-restored")
    }
    @Test fun invalidStoredOptionsFallBackSafely() {
        val prefs=rule.activity.getSharedPreferences("appearance",0)
        prefs.edit().clear().putString("look.style","bad").putInt("look.columns",99).commit()
        assertEquals(LookOptions(),LookOptions.read(prefs))
        for(c in 4..8) { LookOptions(columns=c).save(prefs); assertEquals(c,LookOptions.read(prefs).columns) }
    }
}
