package com.alaa.tvlauncher

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.tv.material3.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.*
import java.time.*
import java.time.format.DateTimeFormatter

@Composable
internal fun WebTile(modifier: Modifier, color: Color = Color(0xFF292B30), click: () -> Unit, content: @Composable BoxScope.()->Unit) {
    val look = LocalLook.current
    val palette = look.palette
    val tileColor = if (look.style == "original" || color != Color(0xFF292B30)) color else palette.panel
    Surface(onClick=click,modifier=modifier,
        shape=ClickableSurfaceDefaults.shape(RoundedCornerShape(if(look.style == "cinema") 4.dp else if(look.style == "soft") 22.dp else 12.dp)),
        colors=ClickableSurfaceDefaults.colors(containerColor=tileColor,focusedContainerColor=if(look.focus=="fill") palette.accent.copy(alpha=.55f) else tileColor,contentColor=Color.White,focusedContentColor=Color.White),
        scale=ClickableSurfaceDefaults.scale(focusedScale=if(look.focus=="outline") 1f else 1.035f),
        border=ClickableSurfaceDefaults.border(focusedBorder=Border(BorderStroke(if(look.focus=="bold") 4.dp else 2.dp,if(look.style=="original") Color.White else palette.accent)))) {
        Box(Modifier.fillMaxSize(),content=content)
    }
}

internal fun categoryApp(category: Int, apps: List<InstalledApp>): InstalledApp? {
    val terms=when(category) {
        0 -> listOf("alaa", "iptv", "player")
        1 -> listOf("netflix", "prime", "shahid", "disney", "alaa")
        2 -> listOf("shahid", "netflix", "prime", "alaa")
        3 -> listOf("youtube", "smarttube")
        else -> listOf("dazn", "sport", "iptv", "alaa")
    }
    return terms.firstNotNullOfOrNull { term -> apps.firstOrNull { it.label.contains(term,true) || it.packageName.contains(term,true) } }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun WebOsHome(apps: List<InstalledApp>, today: PrayerDay?, tomorrow: PrayerDay?, now: ZonedDateTime, series: List<Series>, settings: ()->Unit) {
    val context=LocalContext.current
    var drawer by remember { mutableStateOf(false) }
    var prayerPage by remember { mutableStateOf(false) }
    val prayerFocus=remember { FocusRequester() }
    var notice by remember { mutableStateOf(false) }
    val first=remember { FocusRequester() }
    val close=remember { FocusRequester() }
    LaunchedEffect(drawer) { delay(120); runCatching { if(drawer) close.requestFocus() else first.requestFocus() } }
    if(prayerPage) {
        PrayerPage(today,tomorrow,now,onApps={prayerPage=false;drawer=true},onSettings={prayerPage=false;settings()}) { prayerPage=false }
        return
    }
    BackHandler { if(drawer) drawer=false else notice=false }
    Row(Modifier.fillMaxSize().background(LocalLook.current.palette.background)) {
        Column(Modifier.width(62.dp).fillMaxHeight().padding(top=24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(16.dp)) {
            WebTile(Modifier.size(34.dp).testTag("profile"),click={settings()}) { Text("A",Modifier.align(Alignment.Center),fontWeight=FontWeight.Bold,fontSize=19.sp) }
            WebTile(Modifier.size(34.dp).testTag("notifications"),click={notice=!notice}) { BellIcon(Modifier.align(Alignment.Center).size(21.dp)) }
            WebTile(Modifier.size(34.dp).testTag("home-settings"),click=settings) { Text("⚙",Modifier.align(Alignment.Center),fontSize=23.sp) }
            WebTile(Modifier.size(34.dp).testTag("search-apps"),click={openSafely(context,Intent(Intent.ACTION_SEARCH_LONG_PRESS))}) { Text("⌕",Modifier.align(Alignment.Center),fontSize=27.sp) }
            WebTile(Modifier.size(34.dp).focusRequester(prayerFocus).testTag("open-prayer").semantics { contentDescription="مواقيت الصلاة" },click={prayerPage=true}) { MosqueIcon(Modifier.align(Alignment.Center).size(24.dp)) }
            Spacer(Modifier.weight(1f))
            Text("TV",Modifier.padding(bottom=20.dp),color=Color(0xFF95969C),fontSize=12.sp)
        }
        if(drawer) {
            Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text("Your apps",color=Color.White,fontSize=26.sp,fontWeight=FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    WebTile(Modifier.width(100.dp).height(36.dp).focusRequester(close).testTag("drawer-close"),click={drawer=false}) { Text("رجوع",Modifier.align(Alignment.Center)) }
                }
                if(apps.isEmpty()) Text("No launchable apps installed",color=Color.White)
                if(LocalLook.current.columns != 0) {
                    val count = LocalLook.current.columns
                    LazyVerticalGrid(columns=GridCells.Fixed(count), horizontalArrangement=Arrangement.spacedBy(12.dp), verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(5.dp)) {
                        items(apps,key={it.packageName}) { app ->
                            WebTile(Modifier.fillMaxWidth().height(76.dp).testTag("drawer-${app.packageName}"),click={openSafely(context,app.intent)}) {
                                Column(Modifier.align(Alignment.Center),horizontalAlignment=Alignment.CenterHorizontally) {
                                    if(app.icon!=null) Image(app.icon,app.label,Modifier.size(40.dp))
                                    Text(app.label,fontSize=11.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                } else LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    items(apps,key={it.packageName},contentType={"app-row"}) { app ->
                        WebTile(Modifier.fillMaxWidth().height(48.dp).testTag("drawer-${app.packageName}"),click={openSafely(context,app.intent)}) {
                            Text(app.label,Modifier.align(Alignment.CenterStart).padding(horizontal=18.dp),color=Color.White)
                        }
                    }
                }
            }
        } else Column(Modifier.fillMaxSize().padding(top=18.dp,end=24.dp,bottom=14.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            WebHero(series,today,tomorrow,now,Modifier.fillMaxWidth().weight(1f)) { categoryApp(0,apps)?.let { openSafely(context,it.intent) } ?: run { drawer=true } }
            if(notice) Text("No new launcher notifications",color=Color.White,fontSize=12.sp)
            Row(Modifier.fillMaxWidth().height(38.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                val colors=listOf(0xFF008F87,0xFF7422DD,0xFFD60069,0xFF075DE5,0xFFB3F000)
                listOf("Alaa Player","أفلام","مسلسلات","يوتيوب","رياضة").forEachIndexed { index,label ->
                    WebTile(Modifier.weight(1f).fillMaxHeight().testTag("category-$index"),Color(colors[index]),{
                        categoryApp(index,apps)?.let { openSafely(context,it.intent) } ?: run { drawer=true }
                    }) { Text(label,Modifier.align(Alignment.Center),fontSize=15.sp,fontWeight=FontWeight.Bold,color=if(index==4) Color(0xFF102000) else Color.White) }
                }
            }
            LazyRow(Modifier.fillMaxWidth().height(66.dp).focusRestorer(),contentPadding=PaddingValues(4.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                item(key="all",contentType="action") {
                    WebTile(Modifier.size(58.dp).testTag("all-apps").focusRequester(first),Color(0xFFF0EDE7),{drawer=true}) {
                        Column(Modifier.align(Alignment.Center),horizontalAlignment=Alignment.CenterHorizontally) {
                            Text("▦",color=Color(0xFF27292D),fontSize=24.sp)
                            Text("APPS",color=Color(0xFF27292D),fontSize=9.sp,fontWeight=FontWeight.Bold)
                        }
                    }
                }
                items(apps,key={it.packageName},contentType={"installed-app"}) { app ->
                    WebTile(Modifier.size(58.dp).testTag("app-${app.packageName}"),click={openSafely(context,app.intent)}) {
                        if(app.icon!=null) Image(app.icon,app.label,Modifier.fillMaxSize().padding(6.dp),contentScale=ContentScale.Fit)
                        else Text(app.label,Modifier.align(Alignment.Center).padding(5.dp),fontSize=11.sp,maxLines=2,overflow=TextOverflow.Ellipsis)
                    }
                }
            }
            Text("Top picks for you",color=Color.White,fontSize=18.sp,fontWeight=FontWeight.Medium)
            LazyRow(Modifier.fillMaxWidth().height(114.dp),contentPadding=PaddingValues(4.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                items(series,key={it.id},contentType={"series"}) { show ->
                    WebTile(Modifier.width(190.dp).height(106.dp).testTag("series-${show.id}"),click={ categoryApp(2,apps)?.let { openSafely(context,it.intent) } ?: run { drawer=true } }) {
                        SeriesArt(show,Modifier.fillMaxSize())
                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent,Color(0x80000000)))))
                        Text(show.title,Modifier.align(Alignment.BottomStart).padding(10.dp),color=Color.White,fontSize=14.sp,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis)
                    }
                }
                if(series.isEmpty()) item(key="empty",contentType="status") { Text("Connect to load today's series · saved artwork stays available offline",color=Color(0xFFB4B4BD),fontSize=13.sp) }
            }
        }
    }
}

@Composable
private fun SeriesArt(show: Series?, modifier: Modifier) {
    if(show?.backdrop!=null) Image(remember(show.backdrop){show.backdrop.asImageBitmap()},show.title,modifier,contentScale=ContentScale.Crop)
    else LiteResourceImage(R.drawable.hero,modifier)
}

@Composable
private fun WebHero(series: List<Series>,today: PrayerDay?,tomorrow: PrayerDay?,now: ZonedDateTime,modifier: Modifier,click:()->Unit) {
    val page = 0 // Lite: static hero, no background carousel timer.
    val show=series.getOrNull(page)
    Box(modifier.clip(RoundedCornerShape(18.dp)).testTag("webos-hero")) {
        LookWallpaper(Modifier.fillMaxSize()) { SeriesArt(show,Modifier.fillMaxSize()) }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Transparent,.65f to Color.Transparent,1f to Color(0x65000000))))
        Column(Modifier.align(Alignment.CenterStart).padding(24.dp).fillMaxWidth(.46f),verticalArrangement=Arrangement.spacedBy(7.dp)) {
            Text("Alaa TV",color=Color(0xFFE3DED4),fontSize=12.sp)
            Text(show?.title ?: "Your next great story",color=Color.White,fontSize=32.sp,fontWeight=FontWeight.Bold,maxLines=2,overflow=TextOverflow.Ellipsis)
            Text("Discover today's series",color=Color(0xFFDDDFE4),fontSize=13.sp)
            WebTile(Modifier.width(124.dp).height(32.dp),Color.White,click) { Text("Open Alaa Player",Modifier.align(Alignment.Center),color=Color.Black,fontSize=11.sp,fontWeight=FontWeight.Bold) }
        }
        Column(Modifier.align(Alignment.TopEnd).padding(12.dp).width(390.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xB0000000)).padding(horizontal=14.dp,vertical=8.dp).testTag("clock-glass"),horizontalAlignment=Alignment.End) {
                Text(now.format(webClock),color=Color.White,fontSize=32.sp,fontWeight=FontWeight.Bold)
                Text(now.format(webDate),color=Color(0xFFCBD2DD),fontSize=12.sp)
            }
            PrayerStrip(today,tomorrow,now,Color(0xFFF5F5F7),Color(0xFFB4B6C3))
        }
        Row(Modifier.align(Alignment.BottomStart).padding(start=24.dp,bottom=12.dp),horizontalArrangement=Arrangement.spacedBy(5.dp)) {
            repeat(series.size.coerceAtMost(5)) { index -> Box(Modifier.size(if(index==page) 7.dp else 5.dp).background(if(index==page) Color.White else Color(0xFF777D88),RoundedCornerShape(50))) }
        }
    }
}

private val webClock = DateTimeFormatter.ofPattern("HH:mm")
private val webDate = DateTimeFormatter.ofPattern("EEEE d MMMM",java.util.Locale.forLanguageTag("ar"))

@Composable
private fun BellIcon(modifier: Modifier) {
    Canvas(modifier.semantics { contentDescription = "Notifications" }) {
        val bell = Path().apply {
            moveTo(size.width*.22f,size.height*.72f)
            lineTo(size.width*.27f,size.height*.38f)
            cubicTo(size.width*.29f,size.height*.08f,size.width*.71f,size.height*.08f,size.width*.73f,size.height*.38f)
            lineTo(size.width*.78f,size.height*.72f)
            close()
        }
        drawPath(bell,Color.White,style=Stroke(width=1.6.dp.toPx()))
        drawCircle(Color.White,radius=1.6.dp.toPx(),center=Offset(size.width*.5f,size.height*.88f))
    }
}

@Composable
private fun MosqueIcon(modifier: Modifier) {
    Canvas(modifier) {
        val w=size.width; val h=size.height
        drawRect(Color.White,Offset(w*.12f,h*.5f),androidx.compose.ui.geometry.Size(w*.76f,h*.4f))
        drawOval(Color.White,Offset(w*.25f,h*.24f),androidx.compose.ui.geometry.Size(w*.5f,h*.48f))
        drawRect(Color.White,Offset(w*.1f,h*.13f),androidx.compose.ui.geometry.Size(w*.1f,h*.7f))
        drawRect(Color.White,Offset(w*.8f,h*.13f),androidx.compose.ui.geometry.Size(w*.1f,h*.7f))
        drawRect(Color(0xFF292B30),Offset(w*.43f,h*.65f),androidx.compose.ui.geometry.Size(w*.14f,h*.25f))
    }
}
