package com.alaa.tvlauncher

import android.content.SharedPreferences
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.tv.material3.*
import kotlinx.coroutines.delay
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner

// Missing preferences intentionally mean legacy geometry, artwork and focus.
data class LookOptions(
    val wallpaper: String = "original",
    val style: String = "original",
    val columns: Int = 0,
    val focus: String = "original"
) {
    fun save(prefs: SharedPreferences) {
        prefs.edit().putString("look.wallpaper",wallpaper).putString("look.style",style)
            .putInt("look.columns",columns).putString("look.focus",focus).apply()
    }
    companion object {
        fun read(prefs: SharedPreferences) = LookOptions(
            prefs.getString("look.wallpaper","original").takeIf { it in wallpapers } ?: "original",
            prefs.getString("look.style","original").takeIf { it in styles } ?: "original",
            prefs.getInt("look.columns",0).let { if(it in 4..8) it else 0 },
            prefs.getString("look.focus","original").takeIf { it in focuses } ?: "original")
        val wallpapers = listOf("original","mountain","dusk","forest","rotate")
        val styles = listOf("original","cinema","ocean","soft")
        val focuses = listOf("original","outline","bold","fill")
    }
}
internal val LocalLook = staticCompositionLocalOf { LookOptions() }
internal data class LookPalette(val background: Color, val panel: Color, val accent: Color)
internal val LookOptions.palette get() = when(style) {
    "cinema" -> LookPalette(Color(0xFF100E0B),Color(0xFF30271B),Color(0xFFE5B45F))
    "ocean" -> LookPalette(Color(0xFF041A20),Color(0xFF10343D),Color(0xFF5EE3CF))
    "soft" -> LookPalette(Color(0xFF171923),Color(0xFF34374A),Color(0xFFC5C8F4))
    else -> LookPalette(Color.Black,Color(0xFF292B30),Color.White)
}

@Composable
internal fun LookWallpaper(modifier: Modifier, original: @Composable ()->Unit) {
    val choice = LocalLook.current.wallpaper
    var page by remember(choice) { mutableIntStateOf(0) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(choice,lifecycle) {
        if(choice == "rotate") lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while(true) { delay(300_000); page = (page + 1) % 3 }
        }
    }
    if(choice == "original") original()
    else {
        val selected = if(choice=="rotate") listOf("mountain","dusk","forest")[page] else choice
        Box(modifier) {
            LiteResourceImage(when(selected) {
                "dusk" -> R.drawable.look_dusk
                "forest" -> R.drawable.look_forest
                else -> R.drawable.hero
            },Modifier.matchParentSize())
            Box(Modifier.matchParentSize().background(Color(0x66000000)))
        }
    }
}

@Composable
internal fun LookSettings(look: LookOptions, update:(LookOptions)->Unit, close:()->Unit) {
    val first = remember { FocusRequester() }
    val palette = look.palette
    LaunchedEffect(Unit) { delay(100); first.requestFocus() }
    BackHandler(onBack=close)
    Column(Modifier.fillMaxSize().background(Color(0xFF10151C))
        .verticalScroll(rememberScrollState()).padding(horizontal=38.dp,vertical=22.dp)
        .testTag("look-page"),verticalArrangement=Arrangement.spacedBy(9.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Column {
                Text("الشكل",color=Color.White,fontSize=30.sp,fontWeight=FontWeight.Bold)
                Text("LOOK  /  ALAA TV",color=Color(0xFF89B3BD),fontSize=11.sp,letterSpacing=2.sp)
            }
            Spacer(Modifier.weight(1f))
            LookChip("رجوع","look-close",false,Modifier.width(100.dp).focusRequester(first),click=close)
        }
        Text("تغييرات خفيفة، محفوظة تلقائياً · التصميم الأصلي يبقى كما هو",color=Color(0xFF9FAFBD),fontSize=12.sp)
        LookLabel("الخلفية")
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            listOf("original" to "الأصلية","mountain" to "الجبال","dusk" to "الغروب","forest" to "الغابة","rotate" to "تلقائي · ٥ دقائق").forEach { (id,label) ->
                LookChip(label,"wallpaper-$id",look.wallpaper==id,Modifier.weight(1f)) { update(look.copy(wallpaper=id)) }
            }
        }
        LookLabel("الستايل")
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            listOf("original" to "الأصلي","cinema" to "سينما","ocean" to "المحيط","soft" to "ناعم").forEach { (id,label) ->
                val swatch = look.copy(style=id).palette
                LookChip(label,"style-$id",look.style==id,Modifier.weight(1f),accent=swatch.accent) { update(look.copy(style=id)) }
            }
        }
        LookLabel("عدد التطبيقات بكل صف · كل التطبيقات")
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            LookChip("الأصلي","columns-0",look.columns==0,Modifier.width(112.dp)) { update(look.copy(columns=0)) }
            (4..8).forEach { count ->
                LookChip("$count","columns-$count",look.columns==count,Modifier.width(70.dp)) { update(look.copy(columns=count)) }
            }
        }
        LookLabel("تمييز التطبيق عند التنقل")
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            listOf("original" to "الأصلي","outline" to "إطار ثابت","bold" to "إطار عريض","fill" to "لون واضح").forEach { (id,label) ->
                LookChip(label,"focus-$id",look.focus==id,Modifier.weight(1f)) { update(look.copy(focus=id)) }
            }
        }
        Row(Modifier.padding(top=5.dp),horizontalArrangement=Arrangement.spacedBy(14.dp),verticalAlignment=Alignment.CenterVertically) {
            LookChip("استعادة الشكل الأصلي","look-reset",false,Modifier.width(230.dp)) { update(LookOptions()) }
            Text("بلا ضبابية أو مؤثرات ثقيلة",color=palette.accent,fontSize=12.sp)
        }
    }
}
@Composable private fun LookLabel(text:String) {
    Text(text,Modifier.padding(top=3.dp),color=Color(0xFFCAD6DF),fontSize=13.sp,fontWeight=FontWeight.Bold)
}
@Composable private fun LookChip(
    text:String,tag:String,selected:Boolean,modifier:Modifier,
    accent:Color=Color(0xFF75D9C4),click:()->Unit
) {
    Surface(onClick=click,modifier=modifier.height(44.dp).testTag(tag),
        shape=ClickableSurfaceDefaults.shape(RoundedCornerShape(9.dp)),
        colors=ClickableSurfaceDefaults.colors(containerColor=if(selected) Color(0xFF233D42) else Color(0xFF202A35),focusedContainerColor=Color(0xFF405666)),
        scale=ClickableSurfaceDefaults.scale(focusedScale=1.025f),
        border=ClickableSurfaceDefaults.border(
            border=if(selected) Border(BorderStroke(1.dp,accent)) else Border.None,
            focusedBorder=Border(BorderStroke(2.dp,Color.White)))) {
        Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) {
            Text((if(selected) "✓  " else "")+text,color=if(selected) accent else Color.White,fontSize=13.sp,fontWeight=FontWeight.Medium)
        }
    }
}
