package com.alaa.tvlauncher

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.tv.material3.Text
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Composable
internal fun PrayerPage(today: PrayerDay?,tomorrow: PrayerDay?,now: ZonedDateTime,onApps:()->Unit,onSettings:()->Unit,onBack:()->Unit) {
    val back=remember { FocusRequester() }
    LaunchedEffect(Unit) { back.requestFocus() }
    BackHandler(onBack=onBack)
    Box(Modifier.fillMaxSize().testTag("prayer-page")) {
        LiteResourceImage(R.drawable.prayer_wallpaper,Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x15091932),Color(0x25091932)))))
        Row(Modifier.fillMaxSize()) {
            Column(Modifier.width(88.dp).fillMaxHeight().background(Color(0xBF0D1E42)).padding(horizontal=12.dp,vertical=24.dp),
                horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(18.dp)) {
                Text("◇",color=Color.White,fontSize=33.sp)
                PrayerRailItem("⌂","الرئيسية","prayer-back",back,onBack,true)
                PrayerRailItem("▦","كل التطبيقات","prayer-apps",click=onApps)
                PrayerRailItem("☾","الصلاة","prayer-mosques",click={back.requestFocus()})
                PrayerRailItem("⚙","الإعدادات","prayer-settings",click=onSettings)
            }
            Column(Modifier.weight(1f).fillMaxHeight().padding(start=28.dp,end=32.dp,top=24.dp,bottom=26.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
                Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.End) {
                    Text(prayer12(now.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))),color=Color.White,fontSize=48.sp,fontWeight=FontWeight.Bold)
                    Text(now.format(DateTimeFormatter.ofPattern("EEEE، d MMMM yyyy",java.util.Locale.forLanguageTag("ar"))),color=Color.White,fontSize=15.sp)
                }
                PrayerCalendarPanel(today,tomorrow,now)
            }
        }
    }
}
@Composable
private fun PrayerRailItem(icon:String,label:String,tag:String,focus:FocusRequester?=null,click:()->Unit,active:Boolean=false) {
    WebTile(Modifier.width(64.dp).height(66.dp).then(if(focus!=null) Modifier.focusRequester(focus) else Modifier).testTag(tag),
        color=if(active) Color(0xFFF1F4FF) else Color.Transparent,click=click) {
        Column(Modifier.align(Alignment.Center),horizontalAlignment=Alignment.CenterHorizontally) {
            Text(icon,color=if(active) Color(0xFF142543) else Color.White,fontSize=26.sp)
            Text(label,color=if(active) Color(0xFF142543) else Color.White,fontSize=9.sp)
        }
    }
}
