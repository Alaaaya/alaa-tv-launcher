package com.alaa.tvlauncher

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.HttpURLConnection
import java.time.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import org.json.JSONArray

val berlin: ZoneId = ZoneId.of("Europe/Berlin")
val prayerNames = listOf("الفجر", "الظهر", "العصر", "المغرب", "العشاء")
data class PrayerDay(val date: LocalDate, val times: List<String>, val cached: Boolean)
data class Series(val id: Int, val title: String, val poster: Bitmap?, val backdrop: Bitmap? = poster)
data class Weather(val temperature: Int, val code: Int, val updated: Long)

object PrayerParser {
    // Balanced JSON extraction, including braces and escaped quotes inside strings.
    fun extract(html: String): JSONObject {
        val assignment = Regex("""(?:var\s+|let\s+|const\s+)?confData\s*=\s*""").find(html)
            ?: error("No prayer configuration")
        val start = html.indexOf('{', assignment.range.last + 1)
        require(start >= 0)
        var depth = 0; var quoted = false; var escaped = false
        for (i in start until html.length) {
            val c = html[i]
            if (quoted) {
                if (escaped) escaped = false else if (c == '\\') escaped = true else if (c == '"') quoted = false
            } else {
                if (c == '"') quoted = true
                else if (c == '{') depth++
                else if (c == '}' && --depth == 0) return JSONObject(html.substring(start, i + 1))
            }
        }
        error("Incomplete prayer configuration")
    }
    fun day(config: JSONObject, date: LocalDate, fetchedDate: LocalDate, cached: Boolean): PrayerDay? {
        val source = if (date == fetchedDate) config.optJSONArray("times") else null
        val values = if (source != null && source.length() == 5)
            List(5) { source.getString(it) }
        else {
            val calendar = config.optJSONArray("calendar") ?: return null
            val row = calendar.optJSONObject(date.monthValue - 1)?.optJSONArray(date.dayOfMonth.toString()) ?: return null
            // Mawaqit annual calendar includes sunrise at index 1.
            if (row.length() < 6) return null
            listOf(0, 2, 3, 4, 5).map { row.getString(it) }
        }
        if (values.any { !it.matches(Regex("""\d{2}:\d{2}""")) || runCatching { LocalTime.parse(it) }.isFailure }) return null
        return PrayerDay(date, values, cached)
    }
    fun next(today: PrayerDay?, tomorrow: PrayerDay?, now: ZonedDateTime): Pair<Int, ZonedDateTime>? {
        for (day in listOfNotNull(today, tomorrow))
            day.times.forEachIndexed { index, value ->
                val time = day.date.atTime(LocalTime.parse(value)).atZone(berlin)
                if (time.isAfter(now)) return index to time
            }
        return null
    }
}
class LiveRepository(context: Context) {
    private val prefs = context.getSharedPreferences("live-data", Context.MODE_PRIVATE)
    private fun get(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 12000; c.readTimeout = 12000
        c.setRequestProperty("User-Agent", "AlaaTV/1.1 personal home launcher")
        return try { require(c.responseCode in 200..299); c.inputStream.bufferedReader().use { it.readText() } }
        finally { c.disconnect() }
    }
    fun cachedPrayers(): Pair<PrayerDay?, PrayerDay?> {
        return runCatching {
            val raw = prefs.getString("prayers", null) ?: return null to null
            val date = LocalDate.parse(prefs.getString("prayer-date", ""))
            val now = LocalDate.now(berlin)
            // Annual calendar must not be silently reused in another year.
            if (date.year != now.year) return null to null
            val config = JSONObject(raw)
            PrayerParser.day(config, now, date, true) to PrayerParser.day(config, now.plusDays(1), date, true)
        }.getOrDefault(null to null)
    }
    suspend fun prayers(): Pair<PrayerDay?, PrayerDay?> = withContext(Dispatchers.IO) {
        runCatching {
            val config = PrayerParser.extract(get("https://mawaqit.net/de/arrahma-mulheim"))
            val date = LocalDate.now(berlin)
            val today = PrayerParser.day(config, date, date, false) ?: error("Invalid prayer times")
            prefs.edit().putString("prayers", config.toString()).putString("prayer-date", date.toString()).apply()
            today to PrayerParser.day(config, date.plusDays(1), date, false)
        }.getOrElse { cachedPrayers() }
    }
    fun cachedWeather(): Weather? = runCatching {
        val j = JSONObject(prefs.getString("weather", null) ?: return null)
        Weather(j.getInt("temp"), j.getInt("code"), j.getLong("updated"))
    }.getOrNull()
    suspend fun weather(): Weather? = withContext(Dispatchers.IO) {
        runCatching {
            val j = JSONObject(get("https://api.open-meteo.com/v1/forecast?latitude=51.4353&longitude=6.8799&current=temperature_2m,weather_code&timezone=Europe%2FBerlin")).getJSONObject("current")
            val w = Weather(kotlin.math.round(j.getDouble("temperature_2m")).toInt(), j.getInt("weather_code"), System.currentTimeMillis())
            prefs.edit().putString("weather", JSONObject().put("temp", w.temperature).put("code", w.code).put("updated", w.updated).toString()).apply()
            w
        }.getOrElse { cachedWeather() }
    }

    private val posterDir = File(context.filesDir, "series-posters")
    private fun bytes(url: String): ByteArray {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 12000; c.readTimeout = 15000
        c.setRequestProperty("User-Agent", "AlaaTV/1.2 personal home launcher")
        return try { require(c.responseCode in 200..299); c.inputStream.use { it.readBytes() } } finally { c.disconnect() }
    }
    private fun loadSeries(): List<Series> = runCatching {
        val arr = JSONArray(prefs.getString("series", null) ?: return emptyList())
        List(arr.length().coerceAtMost(5)) { i ->
            val o = arr.getJSONObject(i)
            val f = File(posterDir, o.getInt("id").toString() + ".jpg")
            val poster = if (f.exists()) LiteBitmaps.file(f, 240, 360) else null
            val wide = File(posterDir, "${o.getInt("id")}-wide.jpg")
            Series(o.getInt("id"), o.getString("title"), poster,
                if (wide.exists()) LiteBitmaps.file(wide, 960, 540) ?: poster else poster)
        }
    }.getOrDefault(emptyList())
    fun cachedSeries(): List<Series> = loadSeries()
    /** TVmaze (no key, CC BY-SA): highest-weight scripted English series airing today. Refreshed at most daily. */
    suspend fun series(): List<Series> = withContext(Dispatchers.IO) {
        val today = LocalDate.now(berlin).toString()
        if (prefs.getString("series-date", null) == today) return@withContext loadSeries()
        runCatching {
            val picked = LinkedHashMap<Int, Pair<String, String>>()
            val weights = HashMap<Int, Int>()
            for (url in listOf("https://api.tvmaze.com/schedule/web?date=$today", "https://api.tvmaze.com/schedule?country=US&date=$today")) {
                val eps = JSONArray(get(url))
                for (i in 0 until eps.length()) {
                    val show = eps.getJSONObject(i).optJSONObject("_embedded")?.optJSONObject("show") ?: eps.getJSONObject(i).optJSONObject("show") ?: continue
                    val img = show.optJSONObject("image")?.optString("medium").orEmpty()
                    if (show.optString("type") != "Scripted" || show.optString("language") != "English" || img.isEmpty()) continue
                    val id = show.getInt("id")
                    picked[id] = show.getString("name") to img.replace("http://", "https://")
                    weights[id] = show.optInt("weight", 0)
                }
            }
            val top = picked.keys.sortedByDescending { weights[it] }.take(5)
            require(top.isNotEmpty())
            posterDir.mkdirs()
            val out = JSONArray()
            for (id in top) {
                File(posterDir, "$id.jpg").writeBytes(bytes(picked.getValue(id).second))
                runCatching {
                    val images = JSONArray(get("https://api.tvmaze.com/shows/$id/images"))
                    val wide = (0 until images.length()).map { images.getJSONObject(it) }.firstOrNull { it.optString("type") == "background" }
                    val url = wide?.getJSONObject("resolutions")?.optJSONObject("original")?.optString("url")
                    if (!url.isNullOrEmpty() && url.startsWith("https://static.tvmaze.com/")) File(posterDir,"$id-wide.jpg").writeBytes(bytes(url))
                }
                out.put(JSONObject().put("id", id).put("title", picked.getValue(id).first))
            }
            prefs.edit().putString("series", out.toString()).putString("series-date", today).apply()
            posterDir.listFiles()?.filter { f -> top.none { f.name == "$it.jpg" || f.name == "$it-wide.jpg" } }?.forEach { it.delete() }
            loadSeries()
        }.getOrElse { loadSeries() }
    }
}
fun weatherLabel(code: Int): String = when(code) {
    0 -> "سماء صافية"; 1, 2 -> "غائم جزئياً"; 3 -> "غائم"; 45, 48 -> "ضباب"
    in 51..67, in 80..82 -> "أمطار"; in 71..77, 85, 86 -> "ثلوج"; in 95..99 -> "عواصف رعدية"; else -> "الطقس"
}
