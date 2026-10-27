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
    private val apps=(1..12).map { InstalledApp("fixture.$it","App $it",Intent(),null) }
    private fun home() {
        rule.mainClock.autoAdvance=false
        rule.setContent { LauncherHome(apps,null,null,null,LocalDate.of(2026,10,27).atTime(20,0).atZone(berlin)) }
        rule.mainClock.advanceTimeBy(2000)
    }
    private fun prefs()=rule.activity.getSharedPreferences("appearance",0)
    @Test fun lookPageAndPersistence() {
        prefs().edit().clear().commit()
        assertEquals(LookOptions(),LookOptions.read(prefs()))
        home()
        capture("look20-original")
        press("home-settings")
        press("open-look")
        rule.onNodeWithTag("look-page").assertExists()
        capture("look20-settings")
        press("style-cinema"); press("wallpaper-dusk"); press("columns-6"); press("focus-bold")
        assertEquals(LookOptions("dusk","cinema",6,"bold"),LookOptions.read(prefs()))
        capture("look20-settings-cinema")
        press("look-reset")
        assertEquals(LookOptions(),LookOptions.read(prefs()))
    }
    @Test fun homeCinemaDusk() {
        prefs().edit().clear().commit()
        LookOptions("dusk","cinema",6,"bold").save(prefs())
        home(); capture("look20-cinema")
    }
    @Test fun homeOceanForest() {
        prefs().edit().clear().commit()
        LookOptions("forest","ocean",0,"fill").save(prefs())
        home(); capture("look20-ocean")
    }
    @Test fun homeSoftOutline() {
        prefs().edit().clear().commit()
        LookOptions("mountain","soft",0,"outline").save(prefs())
        home(); capture("look20-soft")
    }
}
