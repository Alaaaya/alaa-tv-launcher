package com.alaa.tvlauncher

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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
                 weather: Weather?, match: Match?, now: ZonedDateTime) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("appearance", Context.MODE_PRIVATE) }
    var theme by remember { mutableStateOf(prefs.getString("theme", "Discover") ?: "Discover") }
    var appearance by remember { mutableStateOf(prefs.getString("appearance", "Auto") ?: "Auto") }
    var settings by remember { mutableStateOf(false) }
    val night = appearance == "Dark" || appearance == "Auto" && isNight(now)
    MaterialTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            if (settings) {
                HomeSettings(theme, appearance, { theme = it; prefs.edit().putString("theme",it).apply() },
                    { appearance = it; prefs.edit().putString("appearance",it).apply() }, { settings = false })
            } else if (theme == "Classic") {
                ClassicHome(apps,today,tomorrow,weather,match,now) { settings = true }
            } else {
                DiscoverHome(apps,today,tomorrow,weather,now,night) { settings = true }
            }
        }
    }
}

@Composable
private fun ActionTile(modifier: Modifier = Modifier, light: Boolean = false,
                       onClick: () -> Unit, content: @Composable BoxScope.() -> Unit) {
    val ink = if (light) Color(0xFF202126) else Color.White
    Surface(onClick = onClick, modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(14.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (light) Color(0xFFFDFDFE) else Color(0xFF25262D),
            focusedContainerColor = if (light) Color.White else Color(0xFF3C3E49),
            contentColor = ink, focusedContentColor = ink),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.045f),
        border = ClickableSurfaceDefaults.border(focusedBorder = Border(BorderStroke(2.dp,if(light) Color(0xFF454B63) else Color.White)))
    ) { Box(Modifier.fillMaxSize(), content = content) }
}

@Composable
private fun DiscoverHome(apps: List<InstalledApp>, today: PrayerDay?, tomorrow: PrayerDay?,
                         weather: Weather?, now: ZonedDateTime, night: Boolean, settings: () -> Unit) {
    val context = LocalContext.current
    val background by animateColorAsState(if(night) Color(0xFF15171E) else Color(0xFFF0F0F3), tween(1500), label="day-night")
    val ink = if(night) Color(0xFFF5F5F7) else Color(0xFF202126)
    val dim = if(night) Color(0xFFB4B6C3) else Color(0xFF626570)
    var tab by remember { mutableIntStateOf(0) }
    var drawer by remember { mutableStateOf(false) }
    val first = remember { FocusRequester() }
    val close = remember { FocusRequester() }
    LaunchedEffect(drawer, apps) { delay(150); runCatching { if(drawer) close.requestFocus() else first.requestFocus() } }
    BackHandler { if(drawer) drawer = false }
    Column(Modifier.fillMaxSize().background(background).padding(horizontal=40.dp, vertical=24.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically) {
            Column {
                Text(if(drawer) "Your apps" else "For You", color=ink, fontSize=36.sp, fontWeight=FontWeight.Bold, letterSpacing=(-1).sp)
                Text("Alaa TV",color=dim,fontSize=11.sp,letterSpacing=1.sp)
            }
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment=Alignment.End) {
                Text(now.format(DateTimeFormatter.ofPattern("HH:mm")),color=ink,fontSize=21.sp,fontWeight=FontWeight.Medium)
                val stale = weather != null && now.toInstant().toEpochMilli() - weather.updated > 3600000
                Text((weather?.let { "${it.temperature}° · Mülheim${if(stale) " · cached" else ""}" } ?: "Mülheim · weather offline"),color=dim,fontSize=11.sp)
            }
            Spacer(Modifier.width(20.dp))
            ActionTile(Modifier.size(38.dp).testTag("all-apps"),!night,{drawer = !drawer}) { Text("▦",Modifier.align(Alignment.Center),fontSize=22.sp,color=ink) }
            Spacer(Modifier.width(10.dp))
            ActionTile(Modifier.size(38.dp).testTag("home-settings"),!night,settings) { Text("⚙",Modifier.align(Alignment.Center),fontSize=22.sp,color=ink) }
        }
        if(drawer) {
            ActionTile(Modifier.width(110.dp).height(38.dp).focusRequester(close),!night,{drawer=false}) { Text("رجوع",Modifier.align(Alignment.Center),color=ink) }
            if(apps.isEmpty()) Text("No launchable apps installed",color=dim)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement=Arrangement.spacedBy(14.dp)) {
                apps.chunked(5).forEach { row ->
                    Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                        row.forEach { app -> DockApp(app, Modifier.width(150.dp).height(85.dp), !night) { openSafely(context,app.intent) } }
                    }
                }
            }
        } else {
            Row(horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                listOf("Explore","Movies","Shows").forEachIndexed { index, label ->
                    ActionTile(Modifier.width(94.dp).height(32.dp).testTag("tab-$index"), !night, {tab=index}) {
                        Text(label,Modifier.align(Alignment.Center),fontSize=13.sp,fontWeight=if(tab==index) FontWeight.Bold else FontWeight.Normal,color=if(tab==index) ink else dim)
                        if(tab==index) Box(Modifier.align(Alignment.BottomCenter).padding(bottom=3.dp).width(22.dp).height(2.dp).background(ink))
                    }
                }
                Spacer(Modifier.weight(1f))
                Text("Sample collection · artwork previews",Modifier.align(Alignment.CenterVertically),color=dim,fontSize=10.sp)
            }
            Row(Modifier.fillMaxWidth().weight(1f).padding(vertical=3.dp),horizontalArrangement=Arrangement.spacedBy(18.dp)) {
                val heading = listOf("Somewhere\nbeyond the ordinary","The great\nescape","Stories worth\nstaying in for")[tab]
                ActionTile(Modifier.weight(2.65f).fillMaxHeight().testTag("hero"),!night,{ drawer=true }) {
                    Image(painterResource(R.drawable.hero),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent,Color(0xDC07151F)))))
                    Column(Modifier.align(Alignment.BottomStart).padding(22.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                        Text("ALAA TV  /  SAMPLE ART",color=Color(0xFFD6E3EC),fontSize=10.sp,letterSpacing=2.sp)
                        Text(heading,color=Color.White,fontWeight=FontWeight.Bold,fontSize=30.sp,lineHeight=32.sp)
                        Text("Open your apps and choose what to watch  ›",color=Color(0xFFE7E9EF),fontSize=11.sp)
                    }
                }
                ArtPoster(Modifier.weight(1f).fillMaxHeight(), tab % 2, listOf("ORBIT","WILD","AFTERLIGHT")[tab]) { drawer = true }
                ArtPoster(Modifier.weight(1f).fillMaxHeight(), (tab+1)%2, listOf("THE\nQUIET EARTH","DEEP\nBLUE","SLOW\nHORIZONS")[tab]) { drawer = true }
            }
            PrayerStrip(today,tomorrow,now,ink,dim)
            LazyRow(Modifier.fillMaxWidth().height(94.dp).background(if(night) Color(0xFF202229) else Color(0xFFE4E4E9),RoundedCornerShape(22.dp)).focusRestorer(),
                contentPadding=PaddingValues(12.dp),horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                items(apps.take(16),key={it.packageName}) { app ->
                    DockApp(app,Modifier.width(120.dp).height(70.dp).then(if(app==apps.firstOrNull()) Modifier.focusRequester(first) else Modifier),!night) {openSafely(context,app.intent)}
                }
                item {
                    ActionTile(Modifier.width(100.dp).height(70.dp).then(if(apps.isEmpty()) Modifier.focusRequester(first) else Modifier),!night,{drawer=true}) {
                        Text("All apps",Modifier.align(Alignment.Center),fontSize=13.sp,color=ink)
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtPoster(modifier: Modifier, kind: Int, title: String, click: () -> Unit) {
    ActionTile(modifier,onClick=click) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Brush.verticalGradient(if(kind==0) listOf(Color(0xFF172537),Color(0xFF04070D)) else listOf(Color(0xFF7D9998),Color(0xFF12312E))))
            if(kind==0) {
                drawCircle(Brush.radialGradient(listOf(Color(0xFFF7CFAA),Color(0xFFC87244))),size.width*.34f,Offset(size.width*.55f,size.height*.31f))
                drawCircle(Color(0xFF101E2E),size.width*.31f,Offset(size.width*.65f,size.height*.26f))
                repeat(26) { i -> drawCircle(Color(0xFFCDD8E8),if(i%3==0) 1.4f else .7f, Offset((i*47%173)/173f*size.width,(i*31%113)/160f*size.height)) }
            } else {
                val p=Path().apply { moveTo(0f,size.height*.74f); lineTo(size.width*.42f,size.height*.22f); lineTo(size.width*.65f,size.height*.55f); lineTo(size.width*.82f,size.height*.35f); lineTo(size.width,size.height*.7f); lineTo(size.width,size.height);lineTo(0f,size.height); close() }
                drawPath(p,Color(0xFF274E46))
                drawCircle(Color(0xFFD6C7A0),size.width*.15f,Offset(size.width*.73f,size.height*.2f))
            }
        }
        Column(Modifier.align(Alignment.BottomStart).padding(15.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
            Text(title,color=Color.White,fontSize=22.sp,lineHeight=23.sp,fontWeight=FontWeight.Bold,letterSpacing=1.sp)
            Text("Artwork preview",color=Color(0xFFC4D1D4),fontSize=9.sp)
        }
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
private fun PrayerStrip(today: PrayerDay?, tomorrow: PrayerDay?, now: ZonedDateTime, ink: Color, dim: Color) {
    val valid=today?.takeIf { it.date==now.toLocalDate() }
    val next=PrayerParser.next(valid,tomorrow,now)
    Row(Modifier.fillMaxWidth().height(31.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("☾",color=ink,fontSize=22.sp)
        Text("مسجد الرحمة",color=ink,fontSize=12.sp,fontWeight=FontWeight.Bold)
        if(valid==null) Text("بانتظار أوقات المسجد",color=dim,fontSize=12.sp)
        else {
            if(next!=null) {
                val seconds=Duration.between(now,next.second).seconds.coerceAtLeast(0)
                Text("${prayerNames[next.first]}  ${next.second.format(DateTimeFormatter.ofPattern("HH:mm"))}",color=ink,fontSize=12.sp)
                Text("%02d:%02d:%02d".format(java.util.Locale.US,seconds/3600,seconds%3600/60,seconds%60),color=ink,fontWeight=FontWeight.Bold,fontSize=13.sp)
            }
            Spacer(Modifier.weight(1f))
            valid.times.forEachIndexed { index,time -> Text("${prayerNames[index]} $time",color=dim,fontSize=10.sp) }
        }
        Text(if(valid?.cached==true) "MAWAQIT · cached" else "MAWAQIT",color=dim,fontSize=8.sp)
    }
}

@Composable
private fun HomeSettings(theme: String, appearance: String, chooseTheme: (String)->Unit, chooseAppearance:(String)->Unit, close:()->Unit) {
    val context=LocalContext.current
    val first=remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(100); first.requestFocus() }
    BackHandler(onBack=close)
    Column(Modifier.fillMaxSize().background(Color(0xFF15171E)).padding(40.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Text("Home settings",color=Color.White,fontSize=30.sp,fontWeight=FontWeight.Bold)
        Text("Theme",color=Color(0xFFBDC2CE),fontSize=16.sp)
        Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
            listOf("Discover","Classic").forEachIndexed { i, name ->
                ActionTile(Modifier.width(200.dp).height(55.dp).testTag("theme-$name").then(if(i==0) Modifier.focusRequester(first) else Modifier),onClick={chooseTheme(name)}) {
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
        Text("Artwork cards are bundled samples, not live movie recommendations.\nApp banners and icons come from installed apps. Prayer times remain live from MAWAQIT.",color=Color(0xFFBDC2CE),fontSize=13.sp)
        ActionTile(Modifier.width(240.dp).height(48.dp),onClick={openSafely(context,Intent(Settings.ACTION_SETTINGS))}) { Text("Android settings",Modifier.align(Alignment.Center),color=Color.White) }
        ActionTile(Modifier.width(160.dp).height(48.dp).testTag("settings-close"),onClick=close) { Text("Done",Modifier.align(Alignment.Center),color=Color.White) }
    }
}
