package com.alaa.tvlauncher

import androidx.compose.ui.graphics.luminance
import android.os.Build
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.isActive
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.Surface
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import java.time.*
import java.time.format.DateTimeFormatter
import kotlin.math.*
import kotlinx.coroutines.delay

// NOAA fractional-year solar position, fixed to the mosque's Mülheim coordinates.
// Independent of the network, DST and cached weather. Civil twilight softens the switch.
fun isNight(now: ZonedDateTime): Boolean {
    val utc = now.withZoneSameInstant(ZoneOffset.UTC)
    val hour = utc.hour + utc.minute / 60.0
    val gamma = 2 * PI / 365 * (utc.dayOfYear - 1 + (hour - 12) / 24)
    val eq = 229.18 * (0.000075 + 0.001868*cos(gamma) - 0.032077*sin(gamma) - 0.014615*cos(2*gamma) - 0.040849*sin(2*gamma))
    val decl = 0.006918 - 0.399912*cos(gamma) + 0.070257*sin(gamma) - 0.006758*cos(2*gamma) + 0.000907*sin(2*gamma) - 0.002697*cos(3*gamma) + 0.00148*sin(3*gamma)
    val angle = Math.toRadians((hour * 60 + eq + 4 * 6.88) / 4 - 180)
    val lat = Math.toRadians(51.43)
    val elevation = Math.toDegrees(asin(sin(lat)*sin(decl)+cos(lat)*cos(decl)*cos(angle)))
    return elevation < -0.833
}

@Composable
fun LauncherHome(apps: List<InstalledApp>, today: PrayerDay?, tomorrow: PrayerDay?,
                 weather: Weather?, now: ZonedDateTime, series: List<Series> = emptyList()) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("appearance", Context.MODE_PRIVATE) }
    var theme by remember { mutableStateOf(prefs.getString("theme", "webOS") ?: "webOS") }
    var appearance by remember { mutableStateOf(prefs.getString("appearance", "Auto") ?: "Auto") }
    var settings by remember { mutableStateOf(false) }
    var lookPage by remember { mutableStateOf(false) }
    var look by remember { mutableStateOf(LookOptions.read(prefs)) }
    val updateLook: (LookOptions)->Unit = { look = it; it.save(prefs) }
    val night = appearance == "Dark" || appearance == "Auto" && isNight(now)
    MaterialTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr, LocalLook provides look) {
            if (lookPage) {
                LookSettings(look, updateLook) { lookPage = false }
            } else if (settings) {
                HomeSettings(theme, appearance, { theme = it; prefs.edit().putString("theme",it).apply() },
                    { appearance = it; prefs.edit().putString("appearance",it).apply() }, { lookPage = true }, { settings = false })
            } else if (theme == "Classic") {
                ClassicHome(apps,today,tomorrow,weather,now) { settings = true }
            } else if (theme == "webOS") {
                WebOsHome(apps,today,tomorrow,now,series) { settings = true }
            } else {
                DiscoverHome(apps,today,tomorrow,weather,now,night,series) { settings = true }
            }
        }
    }
}

@Composable
private fun ActionTile(modifier: Modifier = Modifier, light: Boolean = false,
                       onClick: () -> Unit, content: @Composable BoxScope.() -> Unit) {
    val look = LocalLook.current
    val ink = if (light) Color(0xFF202126) else Color.White
    Surface(onClick = onClick, modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(14.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = if(look.focus=="fill") (if(look.palette.accent.luminance()>.8f) Color(0xFF2F6BFF) else look.palette.accent).copy(alpha=.45f) else Color.Transparent,
            contentColor = ink, focusedContentColor = ink),
        scale = ClickableSurfaceDefaults.scale(focusedScale = if(look.focus=="outline") 1f else 1.045f),
        border = ClickableSurfaceDefaults.border(focusedBorder = Border(BorderStroke(if(look.focus=="bold") 4.dp else 2.dp,if(look.style!="original") look.palette.accent else if(light) Color(0xFF454B63) else Color.White)))
    ) { Box(Modifier.fillMaxSize()) {
        FrostedLayer(light)
        content()
        GlassSheenAndRim()
    } }
}

// Only the bundled static wallpaper layer is blurred. Foreground text and logos
// remain sharp, and focus transforms reuse the layer instead of resampling content.
internal fun glassBlurSupported(sdk: Int) = false // Lite never creates RenderEffect layers.

@Composable
private fun BoxScope.FrostedLayer(light: Boolean) {
    Box(Modifier.matchParentSize().background(if(light) Color(0xE8E1E7EF) else Color(0xEC202B40)))
}

// Adapted from Glass.java's independent sheen/rim layers (Glass TV Home, MIT).
// Drawing AFTER artwork is important: opaque banners must not hide the glass edge.
// Brushes are cached, and this overlay has no pointer/focus handlers.
@Composable
private fun BoxScope.GlassSheenAndRim() {
    // Lite: flat panels, no sheen or extra compositing layers.
}

@Composable
private fun DiscoverHome(apps: List<InstalledApp>, today: PrayerDay?, tomorrow: PrayerDay?,
                         weather: Weather?, now: ZonedDateTime, night: Boolean, series: List<Series>, settings: () -> Unit) {
    val context = LocalContext.current
    val background = if(night) Color(0xC5101725) else Color(0xA8EDF0F3)
    val ink = if(night) Color(0xFFF5F5F7) else Color(0xFF202126)
    val dim = if(night) Color(0xFFB4B6C3) else Color(0xFF626570)
    val lookColumns = LocalLook.current.columns
    val dockWidth = if(lookColumns==0) 150.dp else ((LocalConfiguration.current.screenWidthDp - 150 - (lookColumns-1)*16) / lookColumns).dp
    val dockApps = remember(apps) { apps.take(16) }
    var drawer by remember { mutableStateOf(false) }
    val first = remember { FocusRequester() }
    val close = remember { FocusRequester() }
    LaunchedEffect(drawer, apps) { delay(150); runCatching { if(drawer) close.requestFocus() else first.requestFocus() } }
    BackHandler { if(drawer) drawer = false }
    Box(Modifier.fillMaxSize()) {
    LookWallpaper(Modifier.fillMaxSize()) { LiteResourceImage(R.drawable.hero,Modifier.fillMaxSize()) }
    Box(Modifier.fillMaxSize().drawBehind { drawRect(background) })
    Column(Modifier.fillMaxSize().padding(horizontal=75.dp, vertical=18.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically) {
            Column {
                Text(if(drawer) "Your apps" else "Alaa TV", color=ink, fontSize=24.sp, fontWeight=FontWeight.Medium, fontFamily=FontFamily(Font(R.font.inter_tight)))
                Text(now.format(DateTimeFormatter.ofPattern("HH:mm")),color=ink,fontSize=18.sp,fontFamily=FontFamily(Font(R.font.inter_tight)))
            }
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment=Alignment.End) {
                Text("For You",color=ink,fontSize=16.sp,fontWeight=FontWeight.Medium)
                Text("Trending series",color=dim,fontSize=11.sp)
            }
            Spacer(Modifier.width(20.dp))
            ActionTile(Modifier.size(42.dp).testTag("all-apps"),!night,{drawer = !drawer}) { Text("▦",Modifier.align(Alignment.Center),fontSize=22.sp,color=ink) }
            Spacer(Modifier.width(10.dp))
            ActionTile(Modifier.size(42.dp).testTag("home-settings"),!night,settings) { Text("⚙",Modifier.align(Alignment.Center),fontSize=22.sp,color=ink) }
        }
        if(drawer) {
            ActionTile(Modifier.width(110.dp).height(38.dp).focusRequester(close),!night,{drawer=false}) { Text("رجوع",Modifier.align(Alignment.Center),color=ink) }
            if(apps.isEmpty()) Text("No launchable apps installed",color=dim)
            LazyColumn(Modifier.weight(1f), verticalArrangement=Arrangement.spacedBy(14.dp)) {
                items(apps.chunked(lookColumns.takeIf { it != 0 } ?: 5),key={it.first().packageName},contentType={"app-row"}) { row ->
                    Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                        row.forEach { app -> DockApp(app, Modifier.width(dockWidth).height(85.dp), !night) { openSafely(context,app.intent) } }
                    }
                }
            }
        } else {
            Row(Modifier.fillMaxWidth().weight(1f).padding(vertical=3.dp),horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                if(series.isEmpty()) Text("Trending series appear when online",Modifier.weight(1f),color=dim,fontSize=14.sp)
                else Row(Modifier.weight(1f).fillMaxHeight(),horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                    series.take(4).forEach { show -> SeriesPoster(show,Modifier.weight(1f).fillMaxHeight(),{ drawer=true }) }
                }
                ClockWidget(now,!night,ink,dim,Modifier.width(250.dp).fillMaxHeight())
            }
            PrayerStrip(today,tomorrow,now,ink,dim)
            Box(Modifier.fillMaxWidth().height(111.dp).clip(RoundedCornerShape(17.5.dp))) {
            FrostedLayer(!night)
            LazyRow(Modifier.fillMaxSize().focusRestorer(),
                contentPadding=PaddingValues(horizontal=20.dp,vertical=14.dp),horizontalArrangement=Arrangement.spacedBy(24.dp)) {
                items(dockApps,key={it.packageName},contentType={"installed-app"}) { app ->
                    DockApp(app,Modifier.width(174.5.dp).height(82.5.dp).then(if(app==apps.firstOrNull()) Modifier.focusRequester(first) else Modifier),!night) {openSafely(context,app.intent)}
                }
                item(key="all-apps-action",contentType="action") {
                    ActionTile(Modifier.width(110.dp).height(82.5.dp).then(if(apps.isEmpty()) Modifier.focusRequester(first) else Modifier),!night,{drawer=true}) {
                        Text("All apps",Modifier.align(Alignment.Center),fontSize=13.sp,color=ink)
                    }
                }
            }
        }
    }
    }
    }
}

@Composable
private fun SeriesPoster(show: Series, modifier: Modifier, click: () -> Unit) {
    ActionTile(modifier.testTag("series-${show.id}"),onClick=click) {
        if(show.poster!=null) Image(remember(show.poster){ show.poster.asImageBitmap() },show.title,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
        else Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF26354A),Color(0xFF0A0F18)))))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent,Color(0xD0000000)),startY=250f)))
        Text(show.title,Modifier.align(Alignment.BottomStart).padding(10.dp),color=Color.White,fontSize=13.sp,fontWeight=FontWeight.Bold,maxLines=2,overflow=TextOverflow.Ellipsis)
    }
}

private val arabicDate: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", java.util.Locale.forLanguageTag("ar"))

@Composable
private fun ClockWidget(now: ZonedDateTime, light: Boolean, ink: Color, dim: Color, modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(18.dp)).testTag("clock-glass")) {
        FrostedLayer(light)
        Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally) {
            Text(now.format(DateTimeFormatter.ofPattern("HH:mm",java.util.Locale.US)),color=ink,fontSize=58.sp,fontWeight=FontWeight.Bold,fontFamily=FontFamily(Font(R.font.inter_tight)))
            Text(now.format(arabicDate),color=dim,fontSize=15.sp,textAlign=androidx.compose.ui.text.style.TextAlign.Center)
        }
        GlassSheenAndRim()
    }
}

@Composable
private fun DockApp(app: InstalledApp, modifier: Modifier, light: Boolean, click: () -> Unit) {
    ActionTile(modifier.testTag("app-${app.packageName}"),light,click) {
        val art=app.banner ?: app.icon
        if(art!=null) Image(art,app.label,Modifier.fillMaxSize().then(if(app.banner==null) Modifier.padding(10.dp) else Modifier),contentScale=ContentScale.Fit)
        else Text(app.label,Modifier.align(Alignment.Center).padding(8.dp),fontSize=14.sp,fontWeight=FontWeight.Bold,maxLines=2,overflow=TextOverflow.Ellipsis,color=if(light) Color(0xFF202126) else Color.White)
    }
}

@Composable
internal fun PrayerStrip(today: PrayerDay?, tomorrow: PrayerDay?, now: ZonedDateTime, ink: Color, dim: Color) {
    var elapsed by remember(now) { mutableLongStateOf(0L) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(now,lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            val start = android.os.SystemClock.elapsedRealtime()
            while(isActive) {
                delay(1000)
                elapsed = (android.os.SystemClock.elapsedRealtime() - start) / 1000
            }
        }
    }
    val tick = now.plusSeconds(elapsed)
    val valid=today?.takeIf { it.date==tick.toLocalDate() }
    val next=PrayerParser.next(valid,tomorrow,tick)
    Box(Modifier.fillMaxWidth().height(104.dp).clip(RoundedCornerShape(18.dp)).testTag("prayer-glass")) {
        FrostedLayer(ink != Color(0xFFF5F5F7))
        Column(Modifier.fillMaxSize().padding(horizontal=16.dp,vertical=9.dp),verticalArrangement=Arrangement.SpaceBetween) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Text("☾",color=ink,fontSize=18.sp)
                Text("مسجد الرحمة",color=ink,fontSize=13.sp,fontWeight=FontWeight.Bold)
                if(valid==null) Text("بانتظار أوقات المسجد",color=dim,fontSize=12.sp)
                else if(next!=null) {
                    val seconds=Duration.between(tick,next.second).seconds.coerceAtLeast(0)
                    Text("${prayerNames[next.first]}  ${next.second.format(DateTimeFormatter.ofPattern("HH:mm"))}",color=ink,fontSize=12.sp)
                    Text("%02d:%02d:%02d".format(java.util.Locale.US,seconds/3600,seconds%3600/60,seconds%60),color=ink,fontWeight=FontWeight.Bold,fontSize=14.sp)
                }
                Spacer(Modifier.weight(1f))
                Text(if(valid?.cached==true) "MAWAQIT · cached" else "MAWAQIT",color=dim,fontSize=9.sp)
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                prayerNames.forEachIndexed { index,name ->
                    val highlighted = next?.first==index
                    Column(Modifier.weight(1f).padding(horizontal=3.dp)
                        .background(if(highlighted) ink.copy(alpha=.14f) else Color.Transparent,RoundedCornerShape(10.dp))
                        .then(if(highlighted) Modifier.border(1.dp,ink.copy(alpha=.4f),RoundedCornerShape(10.dp)) else Modifier)
                        .padding(vertical=5.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                        Text(name,color=if(highlighted) ink else dim,fontSize=12.sp,
                            fontWeight=if(highlighted) FontWeight.Bold else FontWeight.Normal)
                        Text(valid?.times?.getOrNull(index) ?: "--:--",color=ink,fontSize=16.sp,
                            fontWeight=if(highlighted) FontWeight.Bold else FontWeight.Medium)
                    }
                }
            }
        }
        GlassSheenAndRim()
    }
}

@Composable
private fun HomeSettings(theme: String, appearance: String, chooseTheme: (String)->Unit, chooseAppearance:(String)->Unit, openLook:()->Unit, close:()->Unit) {
    val context=LocalContext.current
    val first=remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(100); first.requestFocus() }
    BackHandler(onBack=close)
    Column(Modifier.fillMaxSize().background(Color(0xFF15171E)).padding(40.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Text("Home settings",color=Color.White,fontSize=30.sp,fontWeight=FontWeight.Bold)
        ActionTile(Modifier.width(260.dp).height(48.dp).testTag("open-look"),onClick=openLook) { Text("الشكل  /  Look",Modifier.align(Alignment.Center),color=Color.White) }
        Text("Theme",color=Color(0xFFBDC2CE),fontSize=16.sp)
        Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
            listOf("webOS","Discover","Classic").forEachIndexed { i, name ->
                ActionTile(Modifier.width(190.dp).height(55.dp).testTag("theme-$name").then(if(i==0) Modifier.focusRequester(first) else Modifier),onClick={chooseTheme(name)}) {
                    Text("${if(theme==name) "✓  " else ""}$name",Modifier.align(Alignment.Center),color=Color.White)
                }
            }
        }
        Text("Discover appearance · Auto follows sunset in Mülheim",color=Color(0xFFBDC2CE),fontSize=16.sp)
        Row(horizontalArrangement=Arrangement.spacedBy(14.dp)) {
            listOf("Auto","Light","Dark").forEach { name ->
                ActionTile(Modifier.width(150.dp).height(48.dp).testTag("appearance-$name"),onClick={chooseAppearance(name)}) {
                    Text("${if(appearance==name) "✓  " else ""}$name",Modifier.align(Alignment.Center),color=Color.White)
                }
            }
        }
        Text("Layout adapted from tv-makeover / DreamOS (MIT). Inter Tight (OFL).\nGlass sheen/rim adapted from Glass TV Home contributors (MIT).\nTrending series posters and titles: TVmaze (CC BY-SA), refreshed daily.\nApp banners and icons come from installed apps. Prayer times remain live from MAWAQIT.",color=Color(0xFFBDC2CE),fontSize=13.sp)
        ActionTile(Modifier.width(240.dp).height(48.dp),onClick={openSafely(context,Intent(Settings.ACTION_SETTINGS))}) { Text("Android settings",Modifier.align(Alignment.Center),color=Color.White) }
        ActionTile(Modifier.width(160.dp).height(48.dp).testTag("settings-close"),onClick=close) { Text("Done",Modifier.align(Alignment.Center),color=Color.White) }
    }
}
