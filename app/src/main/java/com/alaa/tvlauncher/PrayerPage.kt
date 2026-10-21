package com.alaa.tvlauncher

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
internal fun PrayerPage(today: PrayerDay?,tomorrow: PrayerDay?,now: ZonedDateTime,onBack:()->Unit) {
    val back=remember { FocusRequester() }
    LaunchedEffect(Unit) { back.requestFocus() }
    BackHandler(onBack=onBack)
    Box(Modifier.fillMaxSize().testTag("prayer-page")) {
        Image(painterResource(R.drawable.prayer_wallpaper),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x35030C16),Color(0x60030C16)))))
        Column(Modifier.fillMaxSize().padding(horizontal=48.dp,vertical=28.dp),verticalArrangement=Arrangement.spacedBy(22.dp)) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                WebTile(Modifier.width(100.dp).height(40.dp).focusRequester(back).testTag("prayer-back"),click=onBack) {
                    Text("رجوع",Modifier.align(Alignment.Center),color=Color.White)
                }
                Column(horizontalAlignment=Alignment.End) {
                    Text(now.format(DateTimeFormatter.ofPattern("HH:mm")),color=Color.White,fontSize=54.sp,fontWeight=FontWeight.Bold)
                    Text("مسجد الرحمة · مولهايم",color=Color.White,fontSize=18.sp)
                }
            }
            PrayerCalendarPanel(today,tomorrow,now)
        }
    }
}
