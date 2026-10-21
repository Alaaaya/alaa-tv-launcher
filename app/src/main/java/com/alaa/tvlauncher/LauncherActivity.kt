package com.alaa.tvlauncher

import android.os.Bundle
import android.content.*
import android.provider.Settings
import android.graphics.Bitmap
import android.graphics.Canvas
import android.content.pm.ResolveInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asImageBitmap

data class InstalledApp(val packageName: String, val label: String, val intent: Intent, val icon: androidx.compose.ui.graphics.ImageBitmap?, val banner: androidx.compose.ui.graphics.ImageBitmap? = null)

class LauncherActivity : ComponentActivity() {
    private val revision = mutableIntStateOf(0)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = 5894
        setContent { LauncherScreen(revision.intValue) }
    }
    override fun onResume() { super.onResume(); revision.intValue++ }
}
@Suppress("DEPRECATION")
fun loadApps(context: Context): List<InstalledApp> {
    val pm = context.packageManager
    val tv = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER), 0)
    val mobile = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
    val apps = (tv + mobile).distinctBy { it.activityInfo.packageName }.filter { it.activityInfo.packageName != context.packageName }
    fun rank(info: ResolveInfo): Int {
        val p = info.activityInfo.packageName.lowercase()
        val name = info.loadLabel(pm).toString().lowercase()
        return when {
            p.contains("streamvault") || p.contains("alaa") || name.contains("alaa player") -> 0
            p.contains("youtube") -> 1; p.contains("netflix") -> 2; p.contains("shahid") -> 3
            p.contains("amazonvideo") || p.contains("avod") -> 4
            p.contains("browser") || p.contains("chrome") -> 5
            else -> 6
        }
    }
    return apps.sortedWith(compareBy<ResolveInfo> { rank(it) }.thenBy { it.loadLabel(pm).toString() }).map { info ->
        val drawable = info.loadIcon(pm)
        val banner = runCatching {
            val art = info.activityInfo.loadBanner(pm) ?: pm.getApplicationBanner(info.activityInfo.packageName)
            art?.let { Bitmap.createBitmap(320,180,Bitmap.Config.ARGB_8888).also { b -> art.setBounds(0,0,320,180); art.draw(Canvas(b)) }.asImageBitmap() }
        }.getOrNull()
        val bitmap = runCatching {
            Bitmap.createBitmap(128,128,Bitmap.Config.ARGB_8888).also { b -> drawable.setBounds(0,0,128,128); drawable.draw(Canvas(b)) }.asImageBitmap()
        }.getOrNull()
        InstalledApp(info.activityInfo.packageName, info.loadLabel(pm).toString(),
            Intent(Intent.ACTION_MAIN).setComponent(ComponentName(info.activityInfo.packageName, info.activityInfo.name)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), bitmap, banner)
    }
}
fun openSafely(context: Context, intent: Intent) {
    runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.onFailure {
        android.widget.Toast.makeText(context, "التطبيق غير متاح", android.widget.Toast.LENGTH_SHORT).show()
    }
}
