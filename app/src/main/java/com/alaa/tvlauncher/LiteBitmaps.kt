package com.alaa.tvlauncher

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Bounded decoding for the 1GB box. No full-resolution intermediate textures. */
internal object LiteBitmaps {
    private val cache = object : LruCache<String, Bitmap>(4 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.allocationByteCount
    }
    internal fun sample(width: Int, height: Int, maxWidth: Int, maxHeight: Int): Int {
        var sample = 1
        while (width / sample > maxWidth * 2 || height / sample > maxHeight * 2) sample *= 2
        return sample
    }
    private fun decode(maxWidth: Int, maxHeight: Int, read: (BitmapFactory.Options) -> Bitmap?): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true; inScaled = false }
        read(bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val bitmap = read(BitmapFactory.Options().apply {
            inSampleSize = sample(bounds.outWidth, bounds.outHeight, maxWidth, maxHeight)
            inPreferredConfig = Bitmap.Config.RGB_565
            inScaled = false
        }) ?: return null
        val ratio = minOf(1f, maxWidth.toFloat() / bitmap.width, maxHeight.toFloat() / bitmap.height)
        if (ratio >= 1f) return bitmap
        return Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt().coerceAtLeast(1),
            (bitmap.height * ratio).toInt().coerceAtLeast(1), true).also { if (it !== bitmap) bitmap.recycle() }
    }
    fun file(file: File, width: Int, height: Int): Bitmap? =
        decode(width, height) { BitmapFactory.decodeFile(file.path, it) }
    fun resource(resources: Resources, id: Int, width: Int, height: Int): Bitmap? {
        val key = "$id/$width/$height"
        cache.get(key)?.let { return it }
        return decode(width, height) { BitmapFactory.decodeResource(resources, id, it) }?.also { cache.put(key, it) }
    }
}

@Composable
internal fun LiteResourceImage(id: Int, modifier: Modifier) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(null, id, context) {
        value = withContext(Dispatchers.IO) { LiteBitmaps.resource(context.resources, id, 960, 540) }
    }
    bitmap?.let { Image(remember(it) { it.asImageBitmap() }, null, modifier, contentScale = ContentScale.Crop) }
}
