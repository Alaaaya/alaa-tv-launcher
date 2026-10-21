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
internal data class HijriParts(val year: Int,val month: Int,val day: Int)
internal fun hijriParts(date: LocalDate): HijriParts {
    if(android.os.Build.VERSION.SDK_INT>=24) {
        val cal=android.icu.util.IslamicCalendar(android.icu.util.TimeZone.getTimeZone("UTC"))
        cal.calculationType=android.icu.util.IslamicCalendar.CalculationType.ISLAMIC_UMALQURA
        cal.timeInMillis=date.atTime(12,0).toInstant(ZoneOffset.UTC).toEpochMilli()
        return HijriParts(cal.get(android.icu.util.Calendar.YEAR),cal.get(android.icu.util.Calendar.MONTH)+1,cal.get(android.icu.util.Calendar.DAY_OF_MONTH))
    }
    val h=HijrahDate.from(date)
    return HijriParts(h.get(ChronoField.YEAR),h.get(ChronoField.MONTH_OF_YEAR),h.get(ChronoField.DAY_OF_MONTH))
}
internal fun ramadanDays(date: LocalDate): Long {
    if(hijriParts(date).month==9) return 0
    for(offset in 0L..385L) {
        val h=hijriParts(date.plusDays(offset))
        if(h.month==9 && h.day==1) return offset
    }
    return 0
}
@Composable
private fun GlassCell(modifier: Modifier,active: Boolean=false, content: @Composable ColumnScope.()->Unit) {
    Column(modifier.background(
        Brush.verticalGradient(if(active) listOf(Color(0xE842C590),Color(0xDB18784F)) else listOf(Color(0x594D6C8A),Color(0x55455069))),
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
    val hijri=remember(date) { hijriParts(date) }
    val valid=today?.takeIf { it.date==date }
    val next=PrayerParser.next(valid,tomorrow,tick)
    val seconds=next?.let { Duration.between(tick,it.second).seconds.coerceAtLeast(0) }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(Modifier.fillMaxWidth().testTag("prayer-glass")
            .background(Brush.linearGradient(listOf(Color(0x73394763),Color(0x7A31405B))),RoundedCornerShape(22.dp))
            .border(1.dp,Color.White.copy(alpha=.2f),RoundedCornerShape(22.dp)).padding(14.dp),
            verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                Text("مواقيت الصلاة · مسجد الرحمة مولهايم",color=Color.White,fontSize=19.sp,fontWeight=FontWeight.Bold)
                Text(if(valid?.cached==true) "MAWAQIT · محفوظ" else "MAWAQIT",color=Color(0xFFB9D6D0),fontSize=10.sp)
            }
            Row(Modifier.fillMaxWidth().height(120.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                prayerNames.forEachIndexed { i,name ->
                    val active=next?.first==i
                    val time=if(next?.first==i && next.second.toLocalDate()!=date) tomorrow?.times?.getOrNull(i) else valid?.times?.getOrNull(i)
                    GlassCell(Modifier.weight(1f).fillMaxHeight().testTag("prayer-tile-$i"),active) {
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center,verticalAlignment=Alignment.CenterVertically) {
                            PrayerSymbol(i,Modifier.size(27.dp))
                            Spacer(Modifier.width(7.dp))
                            Text(name,color=Color.White,fontSize=16.sp,fontWeight=FontWeight.Bold)
                        }
                        Spacer(Modifier.height(6.dp))
                        if(active && seconds!=null) Text("بعد ${seconds/3600} س ${seconds%3600/60} د",
                            Modifier.background(Color(0xEAEDFFF6),RoundedCornerShape(50)).padding(horizontal=8.dp,vertical=3.dp),
                            color=Color(0xFF167350),fontSize=10.sp,fontWeight=FontWeight.Bold)
                        else Spacer(Modifier.height(22.dp))
                        Text(time?.let(::prayer12) ?: "--:--",color=Color.White,fontSize=27.sp,fontWeight=FontWeight.Bold)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().height(100.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                GlassCell(Modifier.weight(1f).fillMaxHeight().testTag("hijri-date")) {
                    Text("☾  التقويم الهجري",color=Color(0xFFD4E1E6),fontSize=11.sp)
                    Text("${hijri.day}",color=Color.White,fontSize=28.sp,fontWeight=FontWeight.Bold)
                    Text("${hijriMonths[hijri.month-1]} ${hijri.year} هـ",color=Color.White,fontSize=12.sp)
                    Text(weekDays[date.dayOfWeek.value-1],color=Color(0xFFD4E1E6),fontSize=10.sp)
                }
                GlassCell(Modifier.weight(1f).fillMaxHeight().testTag("gregorian-date")) {
                    Text("▦  التقويم الميلادي",color=Color(0xFFD4E1E6),fontSize=11.sp)
                    Text("${date.dayOfMonth}",color=Color.White,fontSize=28.sp,fontWeight=FontWeight.Bold)
                    Text("${gregMonths[date.monthValue-1]} ${date.year}",color=Color.White,fontSize=12.sp)
                    Text(weekDays[date.dayOfWeek.value-1],color=Color(0xFFD4E1E6),fontSize=10.sp)
                }

                GlassCell(Modifier.weight(1f).fillMaxHeight().testTag("ramadan-countdown")) {
                    Text("☾  باقي على رمضان",color=Color(0xFFFFD884),fontSize=17.sp)
                    Text("${ramadanDays(date)} أيام",color=Color.White,fontSize=28.sp,fontWeight=FontWeight.Bold)
                    Text("تقويم أم القرى · حسب رؤية الهلال",color=Color(0xFFD4E1E6),fontSize=10.sp)
                }

            }
        }
    }
}
