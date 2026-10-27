package com.alaa.tvlauncher

import androidx.compose.ui.graphics.luminance
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.Surface
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import kotlinx.coroutines.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.time.*
import java.time.format.DateTimeFormatter

private val Gold = Color(0xFFF1BB63)
private val Muted = Color(0xFFB2B9C9)
private val Shape = RoundedCornerShape(16.dp)

@Composable
fun LauncherScreen(revision: Int) {
    val context = LocalContext.current
    val repo = remember(context) { LiveRepository(context) }
    var apps by remember { mutableStateOf(emptyList<InstalledApp>()) }
    var today by remember { mutableStateOf<PrayerDay?>(null) }
    var tomorrow by remember { mutableStateOf<PrayerDay?>(null) }
    val weather: Weather? = null
    var series by remember { mutableStateOf(emptyList<Series>()) }
    var now by remember { mutableStateOf(ZonedDateTime.now(berlin)) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(revision, lifecycle) {
      lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        apps = withContext(Dispatchers.IO) { loadApps(context) }
        val saved = withContext(Dispatchers.IO) { repo.cachedPrayers() }; today = saved.first; tomorrow = saved.second
        launch { while (isActive) { now = ZonedDateTime.now(berlin); delay(60_000 - System.currentTimeMillis() % 60_000) } }
        launch {
            while (isActive) {
                val days = repo.prayers(); today = days.first; tomorrow = days.second
                delay(15 * 60 * 1000L)
            }
        }
        launch { series = withContext(Dispatchers.IO) { repo.cachedSeries() }; while (isActive) { series = repo.series(); delay(6 * 60 * 60 * 1000L) } }

      }
    }
    LauncherHome(apps, today, tomorrow, weather, now, series)
}

@Composable
private fun Tile(modifier: Modifier = Modifier, onClick: () -> Unit, content: @Composable BoxScope.() -> Unit) {
    val look = LocalLook.current
    Surface(
        onClick = onClick, modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(shape = Shape),
        colors = ClickableSurfaceDefaults.colors(containerColor = Color(0xFF1B1F27), focusedContainerColor = if(look.focus=="fill") (if(look.palette.accent.luminance()>.8f) Color(0xFF2F6BFF) else look.palette.accent).copy(alpha=.5f) else Color(0xFF242B38), contentColor = Color.White, focusedContentColor = Color.White),
        scale = ClickableSurfaceDefaults.scale(focusedScale = if(look.focus=="outline") 1f else 1.035f),
        border = ClickableSurfaceDefaults.border(focusedBorder = Border(BorderStroke(if(look.focus=="bold") 4.dp else 2.dp, if(look.style=="original") Color.White else look.palette.accent)))
    ) { Box(Modifier.fillMaxSize(), content = content) }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ClassicHome(
    apps: List<InstalledApp>, today: PrayerDay?, tomorrow: PrayerDay?,
    weather: Weather?, now: ZonedDateTime, onSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    var allApps by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    val first = remember { FocusRequester() }
    val closeFocus = remember { FocusRequester() }
    var hero by remember { mutableIntStateOf(0) }

    LaunchedEffect(apps, allApps) { delay(150); runCatching { if (allApps) closeFocus.requestFocus() else first.requestFocus() } }
    BackHandler { if (allApps) { allApps = false; search = "" } }
    MaterialTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                if (!allApps) {
                Column(Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth().height(34.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(30.dp).clip(RoundedCornerShape(10.dp)).background(Gold), contentAlignment = Alignment.Center) {
                            Text("A", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 20.sp)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text("Alaa TV", fontSize = 21.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Tile(Modifier.size(34.dp), { search = ""; allApps = true }) { Text("⌕", Modifier.align(Alignment.Center), fontSize = 26.sp) }
                        Spacer(Modifier.width(14.dp))
                        Tile(Modifier.size(34.dp), { onSettings() }) { Text("⚙", Modifier.align(Alignment.Center), fontSize = 22.sp) }
                        Spacer(Modifier.width(20.dp))
                        Text(now.format(DateTimeFormatter.ofPattern("HH:mm")), color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Medium)
                    }
                    Box(Modifier.fillMaxWidth().weight(1f).clip(Shape)) {
                        LookWallpaper(Modifier.fillMaxSize()) { LiteResourceImage(R.drawable.hero, Modifier.fillMaxSize()) }
                        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xEB050C14), Color(0x60050C14), Color.Transparent))))
                        Column(Modifier.align(Alignment.CenterStart).padding(24.dp).widthIn(max = 410.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("ALAA TV  /  YOUR HOME", fontSize = 11.sp, color = Gold, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            run { val page = 0
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(listOf("عالمك على شاشة واحدة", "وقتك… إلك", "كل اللي بتحبّه، هون")[page], fontSize = 30.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    Text(listOf("تطبيقاتك المفضلة ومواقيت الصلاة", "افتح تطبيقك المفضل واستمتع بوقتك", "بداية بسيطة لكل سهرة حلوة")[page], fontSize = 13.sp, color = Color(0xFFE0E3E9))
                                }
                            }
                            Spacer(Modifier.height(3.dp))
                            Tile(Modifier.width(132.dp).height(34.dp), { allApps = true }) {
                                Text("▶  افتح التطبيقات", Modifier.align(Alignment.Center), fontSize = 12.sp)
                            }
                        }
                        Row(Modifier.align(Alignment.BottomEnd).padding(15.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(3) { i -> Box(Modifier.height(4.dp).width(if (i == hero) 20.dp else 5.dp).clip(Shape).background(if (i == hero) Color.White else Color(0x667B8A9D))) }
                        }
                    }
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().height(106.dp).focusRestorer(),
                        contentPadding = PaddingValues(5.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        itemsIndexed(apps.take(12), key = { _, a -> a.packageName }, contentType = { _, _ -> "app" }) { index, app ->
                            AppTile(app, Modifier.then(if (index == 0) Modifier.focusRequester(first) else Modifier)) { openSafely(context, app.intent) }
                        }
                        item(key = "all", contentType = "all") {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(94.dp)) {
                                Tile(Modifier.size(76.dp).testTag("all-apps").then(if (apps.isEmpty()) Modifier.focusRequester(first) else Modifier), { allApps = true }) {
                                    Text("▦", Modifier.align(Alignment.Center), fontSize = 36.sp, color = Gold)
                                }
                                Text("كل التطبيقات", Modifier.padding(top = 6.dp), color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth().height(148.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PrayerCard(today, tomorrow, now, Modifier.weight(1.15f))

                    }
                }
                }
                if (allApps) {
                    Column(Modifier.fillMaxSize().background(Color(0xFC090C12)).padding(32.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("كل التطبيقات", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 26.sp)
                            Spacer(Modifier.weight(1f))
                            Tile(Modifier.width(100.dp).height(38.dp).focusRequester(closeFocus), { allApps = false; search = "" }) { Text("رجوع", Modifier.align(Alignment.Center), fontSize = 14.sp) }
                        }
                        Spacer(Modifier.height(16.dp))
                        BasicTextField(value = search, onValueChange = { search = it }, singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 18.sp),
                            cursorBrush = SolidColor(Gold),
                            modifier = Modifier.fillMaxWidth().background(Color(0xFF202735), Shape).padding(16.dp),
                            decorationBox = { inner -> Box { if (search.isEmpty()) Text("ابحث عن تطبيق…", color = Muted, fontSize = 18.sp); inner() } })
                        Spacer(Modifier.height(20.dp))
                        val filtered = remember(apps, search) { apps.filter { it.label.contains(search, true) } }
                        if (filtered.isEmpty()) Text("ما في تطبيقات مطابقة", color = Muted)
                        LazyVerticalGrid(columns = if(LocalLook.current.columns==0) GridCells.Adaptive(115.dp) else GridCells.Fixed(LocalLook.current.columns), contentPadding = PaddingValues(6.dp), verticalArrangement = Arrangement.spacedBy(20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            items(filtered, key = { it.packageName }, contentType = { "app" }) { app ->
                                AppTile(app) { openSafely(context, app.intent) }
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun AppTile(app: InstalledApp, modifier: Modifier = Modifier, click: () -> Unit) {
    Column(modifier.width(94.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Tile(Modifier.size(76.dp), click) {
            if (app.icon != null) Image(app.icon, app.label, Modifier.fillMaxSize().padding(7.dp), contentScale = ContentScale.Fit)
            else Text(app.label.take(1), Modifier.align(Alignment.Center), color = Gold, fontSize = 30.sp)
        }
        Text(app.label, Modifier.padding(top = 6.dp).fillMaxWidth(), color = Color.White, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

@Composable
private fun InfoCard(modifier: Modifier, colors: List<Color>, content: @Composable ColumnScope.() -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(modifier.fillMaxHeight().clip(Shape).background(Brush.linearGradient(colors)).padding(14.dp), content = content)
    }
}
@Composable
private fun PrayerCard(today: PrayerDay?, tomorrow: PrayerDay?, now: ZonedDateTime, modifier: Modifier) {
    InfoCard(modifier, listOf(Color(0xFF11254A), Color(0xFF080F23))) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("مواقيت الصلاة", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("☾", color = Gold, fontSize = 23.sp)
        }
        Text("مسجد الرحمة · مولهايم", color = Muted, fontSize = 10.sp)
        val next = PrayerParser.next(today, tomorrow, now)
        if (today == null || today.date != now.toLocalDate()) {
            Text("بانتظار أوقات المسجد", color = Gold, fontSize = 15.sp, modifier = Modifier.padding(top = 14.dp))
        } else {
            if (next != null) {
                val seconds = Duration.between(now, next.second).seconds.coerceAtLeast(0)
                val countdown = "%02d:%02d:%02d".format(java.util.Locale.US, seconds / 3600, (seconds % 3600) / 60, seconds % 60)
                Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("القادمة  ${prayerNames[next.first]}", color = Gold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) { Text(countdown, color = Gold, fontSize = 17.sp, fontWeight = FontWeight.Bold) }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                today.times.forEachIndexed { index, time ->
                    val highlighted = next?.first == index
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(prayerNames[index], color = if (highlighted) Gold else Muted, fontSize = 10.sp)
                        Text(time, color = if (highlighted) Gold else Color.White, fontSize = 12.sp)
                    }
                }
            }
            Text(if (today.cached) "MAWAQIT · محفوظ دون اتصال" else "MAWAQIT · أوقات المسجد", color = Muted, fontSize = 8.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
@Composable
private fun WeatherCard(weather: Weather?, now: ZonedDateTime, modifier: Modifier) {
    InfoCard(modifier, listOf(Color(0xFF234E80), Color(0xFF102342))) {
        Text("الطقس", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(weather?.let { "${it.temperature}°" } ?: "—°", color = Color.White, fontSize = 39.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text(if ((weather?.code ?: 0) >= 51) "☂" else "☁", color = Color(0xFFDCEAFF), fontSize = 42.sp)
        }
        Text(weather?.let { weatherLabel(it.code) } ?: "بانتظار اتصال الإنترنت", color = Color.White, fontSize = 12.sp)
        Text("Mülheim an der Ruhr", color = Color(0xFFD0DBEB), fontSize = 11.sp)
        val stale = weather != null && now.toInstant().toEpochMilli() - weather.updated > 60 * 60 * 1000
        Text(if (stale) "Open-Meteo · بيانات محفوظة" else "Open-Meteo · CC BY 4.0", color = Muted, fontSize = 8.sp)
    }
}
