package com.alaa.tvlauncher

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.tv.material3.Text
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.*
import java.time.*
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit

internal fun prayer12(value: String): String {
    val t=LocalTime.parse(value)
    return "${if(t.hour%12==0) 12 else t.hour%12}:${t.minute.toString().padStart(2,'0')} ${if(t.hour<12) "ص" else "م"}"
}
private val hijriMonths=listOf("محرم","صفر","ربيع الأول","ربيع الآخر","جمادى الأولى","جمادى الآخرة","رجب","شعبان","رمضان","شوال","ذو القعدة","ذو الحجة")
private val gregMonths=listOf("يناير","فبراير","مارس","أبريل","مايو","يونيو","يوليو","أغسطس","سبتمبر","أكتوبر","نوفمبر","ديسمبر")
private val weekDays=listOf("الاثنين","الثلاثاء","الأربعاء","الخميس","الجمعة","السبت","الأحد")
internal fun ramadanDays(date: LocalDate): Long {
    val h=HijrahDate.from(date)
    var year=h.get(ChronoField.YEAR)
    var start=LocalDate.from(HijrahDate.of(year,9,1))
    if(date.isAfter(start) && h.get(ChronoField.MONTH_OF_YEAR)!=9) {
        year++;start=LocalDate.from(HijrahDate.of(year,9,1))
    }
    return ChronoUnit.DAYS.between(date,start).coerceAtLeast(0)
}
@Composable
private fun GlassCell(modifier: Modifier,active: Boolean=false, content: @Composable ColumnScope.()->Unit) {
    Column(modifier.background(
        Brush.verticalGradient(if(active) listOf(Color(0xE842C590),Color(0xDB18784F)) else listOf(Color(0xA34D555C),Color(0x76202730))),
        RoundedCornerShape(14.dp))
        .border(1.dp,if(active) Color(0xFF90FFD2) else Color.White.copy(alpha=.22f),RoundedCornerShape(14.dp))
        .padding(10.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center,content=content)
}
@Composable
internal fun PrayerCalendarPanel(today: PrayerDay?,tomorrow: PrayerDay?,now: ZonedDateTime) {
    var elapsed by remember(now) { mutableLongStateOf(0) }
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(now,lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            val start=android.os.SystemClock.elapsedRealtime()
            while(isActive) { delay(1000); elapsed=(android.os.SystemClock.elapsedRealtime()-start)/1000 }
        }
    }
    val tick=now.plusSeconds(elapsed)
    val date=tick.toLocalDate()
    val hijri=remember(date) { HijrahDate.from(date) }
    val valid=today?.takeIf { it.date==date }
    val next=PrayerParser.next(valid,tomorrow,tick)
    val seconds=next?.let { Duration.between(tick,it.second).seconds.coerceAtLeast(0) }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(Modifier.fillMaxWidth().testTag("prayer-glass")
            .background(Brush.linearGradient(listOf(Color(0xDB303C45),Color(0xC20E171F))),RoundedCornerShape(20.dp))
            .border(1.dp,Color.White.copy(alpha=.2f),RoundedCornerShape(20.dp)).padding(14.dp),
            verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                Text("مواقيت الصلاة · مسجد الرحمة مولهايم",color=Color.White,fontSize=17.sp,fontWeight=FontWeight.Bold)
                Text(if(valid?.cached==true) "MAWAQIT · محفوظ" else "MAWAQIT",color=Color(0xFFB9D6D0),fontSize=10.sp)
            }
            Row(Modifier.fillMaxWidth().height(104.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                prayerNames.forEachIndexed { i,name ->
                    val active=next?.first==i
                    val time=if(next?.first==i && next.second.toLocalDate()!=date) tomorrow?.times?.getOrNull(i) else valid?.times?.getOrNull(i)
                    GlassCell(Modifier.weight(1f).fillMaxHeight().testTag("prayer-tile-$i"),active) {
                        Text(name,color=Color.White,fontSize=14.sp)
                        Text(time?.let(::prayer12) ?: "--:--",color=Color.White,fontSize=24.sp,fontWeight=FontWeight.Bold)
                        if(active && seconds!=null) Text("بعد ${seconds/3600} س ${seconds%3600/60} د",
                            Modifier.background(Color(0x65003020),RoundedCornerShape(50)).padding(horizontal=9.dp,vertical=3.dp),
                            color=Color.White,fontSize=11.sp)
                        else Spacer(Modifier.height(19.dp))
                    }
                }
            }
            Row(Modifier.fillMaxWidth().height(100.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                GlassCell(Modifier.weight(1f).fillMaxHeight().testTag("hijri-date")) {
                    Text("التقويم الهجري",color=Color(0xFFD4E1E6),fontSize=11.sp)
                    Text("${hijri.get(ChronoField.DAY_OF_MONTH)}",color=Color.White,fontSize=28.sp,fontWeight=FontWeight.Bold)
                    Text("${hijriMonths[hijri.get(ChronoField.MONTH_OF_YEAR)-1]} ${hijri.get(ChronoField.YEAR)} هـ",color=Color.White,fontSize=12.sp)
                    Text(weekDays[date.dayOfWeek.value-1],color=Color(0xFFD4E1E6),fontSize=10.sp)
                }
                GlassCell(Modifier.weight(1f).fillMaxHeight().testTag("gregorian-date")) {
                    Text("التقويم الميلادي",color=Color(0xFFD4E1E6),fontSize=11.sp)
                    Text("${date.dayOfMonth}",color=Color.White,fontSize=28.sp,fontWeight=FontWeight.Bold)
                    Text("${gregMonths[date.monthValue-1]} ${date.year}",color=Color.White,fontSize=12.sp)
                    Text(weekDays[date.dayOfWeek.value-1],color=Color(0xFFD4E1E6),fontSize=10.sp)
                }

                GlassCell(Modifier.weight(1.4f).fillMaxHeight().testTag("ramadan-countdown")) {
                    Text("☾  باقي على رمضان",color=Color.White,fontSize=16.sp)
                    Text("${ramadanDays(date)} أيام",color=Color.White,fontSize=28.sp,fontWeight=FontWeight.Bold)
                    Text("تقويم أم القرى · حسب رؤية الهلال",color=Color(0xFFD4E1E6),fontSize=10.sp)
                }

            }
        }
    }
}
