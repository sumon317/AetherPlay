package com.example.ui.components

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.player.MediaSafety
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import kotlin.math.max
import kotlin.math.min

private const val MAX_THUMBNAIL_DIM_PX = 320
private const val DISK_CACHE_DIR = "thumbs_v1"
private const val DISK_CACHE_MAX_FILES = 250
private const val DISK_CACHE_TRIM_TARGET = 180
private val thumbnailExtractorSemaphore = Semaphore(permits = 4)

internal object ThumbnailCache {
    private val maxCacheKb: Int = ((Runtime.getRuntime().maxMemory() / 1024L) / 16L)
        .toInt()
        .coerceIn(4 * 1024, 16 * 1024)

    private val cache = object : LruCache<String, Bitmap>(maxCacheKb) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return (value.allocationByteCount / 1024).coerceAtLeast(1)
        }
    }

    fun get(key: String): Bitmap? = cache.get(key)?.takeIf { !it.isRecycled }

    fun put(key: String, bitmap: Bitmap) {
        if (!bitmap.isRecycled) {
            cache.put(key, bitmap)
        }
    }

    fun clear() {
        cache.evictAll()
    }
}

internal fun decodeSampledBitmap(bytes: ByteArray, maxDimPx: Int = MAX_THUMBNAIL_DIM_PX): Bitmap? {
    if (bytes.isEmpty()) return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sampleSize = 1
    while ((bounds.outWidth / sampleSize) > maxDimPx * 2 || (bounds.outHeight / sampleSize) > maxDimPx * 2) {
        sampleSize *= 2
    }

    val decodeOpts = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = Bitmap.Config.RGB_565
    }
    val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts) ?: return null
    return scaleDownIfNeeded(decoded, maxDimPx)
}

private fun scaleDownIfNeeded(bitmap: Bitmap, maxDimPx: Int = MAX_THUMBNAIL_DIM_PX): Bitmap {
    val w = bitmap.width
    val h = bitmap.height
    if (w <= maxDimPx && h <= maxDimPx) return bitmap
    val scale = maxDimPx.toFloat() / max(w, h).toFloat()
    val targetW = (w * scale).toInt().coerceAtLeast(1)
    val targetH = (h * scale).toInt().coerceAtLeast(1)
    val scaled = Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    if (scaled !== bitmap) {
        bitmap.recycle()
    }
    return scaled
}

private fun isMostlySolidColor(bitmap: Bitmap): Boolean {
    if (bitmap.isRecycled || bitmap.width < 4 || bitmap.height < 4) return false
    var minLuma = 255
    var maxLuma = 0
    var sumLuma = 0
    var count = 0
    for (row in 1..4) {
        val y = (bitmap.height * row) / 5
        for (col in 1..4) {
            val x = (bitmap.width * col) / 5
            val px = bitmap.getPixel(x, y)
            val luma = (AndroidColor.red(px) * 30 + AndroidColor.green(px) * 59 + AndroidColor.blue(px) * 11) / 100
            minLuma = min(minLuma, luma)
            maxLuma = max(maxLuma, luma)
            sumLuma += luma
            count++
        }
    }
    if (count == 0) return false
    val spread = maxLuma - minLuma
    val avg = sumLuma / count
    return spread <= 12 && (avg <= 16 || avg >= 238)
}

private fun sha256Hex(key: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(key.toByteArray(Charsets.UTF_8))
    return buildString(digest.size * 2) {
        for (b in digest) {
            val v = b.toInt() and 0xFF
            append("0123456789abcdef"[v ushr 4])
            append("0123456789abcdef"[v and 0x0F])
        }
    }
}

private fun diskCacheFile(appContext: Context, uriString: String): File {
    val dir = File(appContext.cacheDir, DISK_CACHE_DIR)
    if (!dir.exists()) {
        dir.mkdirs()
    }
    return File(dir, "${sha256Hex(uriString)}.jpg")
}

private fun readFromDiskCache(appContext: Context, uriString: String): Bitmap? {
    return runCatching {
        val file = diskCacheFile(appContext, uriString)
        if (!file.exists() || file.length() <= 0L) return@runCatching null
        val opts = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        BitmapFactory.decodeFile(file.absolutePath, opts)?.takeIf { !it.isRecycled }
    }.getOrNull()
}

private fun writeToDiskCache(appContext: Context, uriString: String, bitmap: Bitmap) {
    if (bitmap.isRecycled) return
    runCatching {
        val target = diskCacheFile(appContext, uriString)
        val tmp = File(target.parentFile, "${target.name}.tmp")
        FileOutputStream(tmp).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 82, out)
        }
        if (target.exists()) target.delete()
        if (!tmp.renameTo(target)) {
            tmp.delete()
        }
        val dir = target.parentFile ?: return@runCatching
        val files = dir.listFiles() ?: return@runCatching
        if (files.size > DISK_CACHE_MAX_FILES) {
            files.sortedBy { it.lastModified() }
                .take(files.size - DISK_CACHE_TRIM_TARGET)
                .forEach { it.delete() }
        }
    }
}

private fun extractScaledFrame(retriever: MediaMetadataRetriever, timeUs: Long): Bitmap? {
    val raw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        retriever.getScaledFrameAtTime(
            timeUs,
            MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
            MAX_THUMBNAIL_DIM_PX,
            MAX_THUMBNAIL_DIM_PX
        )
    } else {
        null
    } ?: retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
    return raw?.let { scaleDownIfNeeded(it, MAX_THUMBNAIL_DIM_PX) }
}

private suspend fun extractBitmap(
    appContext: Context,
    uriString: String,
    isVideo: Boolean,
    isStream: Boolean
): Bitmap? {
    if (isStream) return null
    ThumbnailCache.get(uriString)?.let { return it }

    return withContext(Dispatchers.IO) {
        val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return@withContext null
        if (!MediaSafety.isSafeExternalMediaUri(appContext, uri)) return@withContext null

        readFromDiskCache(appContext, uriString)?.let { cached ->
            ThumbnailCache.put(uriString, cached)
            return@withContext cached
        }

        thumbnailExtractorSemaphore.withPermit {
            ThumbnailCache.get(uriString)?.let { return@withPermit it }
            readFromDiskCache(appContext, uriString)?.let { cached ->
                ThumbnailCache.put(uriString, cached)
                return@withPermit cached
            }

            var bmp: Bitmap? = null

            if (isVideo && uri.scheme == ContentResolver.SCHEME_CONTENT && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val osThumb = runCatching {
                    appContext.contentResolver.loadThumbnail(
                        uri,
                        Size(MAX_THUMBNAIL_DIM_PX, MAX_THUMBNAIL_DIM_PX),
                        null
                    )
                }.getOrNull()
                if (osThumb != null && !osThumb.isRecycled) {
                    val scaled = scaleDownIfNeeded(osThumb, MAX_THUMBNAIL_DIM_PX)
                    if (!isMostlySolidColor(scaled)) {
                        bmp = scaled
                    } else {
                        bmp = scaled // Keep as fallback in case retriever also yields solid/null
                    }
                }
            }

            if (bmp == null || isMostlySolidColor(bmp)) {
                val retriever = MediaMetadataRetriever()
                val retrieved = try {
                    if (uri.scheme == ContentResolver.SCHEME_FILE && !uri.path.isNullOrBlank()) {
                        retriever.setDataSource(uri.path)
                    } else {
                        retriever.setDataSource(appContext, uri)
                    }
                    if (isVideo) {
                        val durationUs = (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                            ?.toLongOrNull() ?: 0L) * 1000L
                        var best = if (bmp == null) extractScaledFrame(retriever, 1_000_000L) else null
                        if (best == null || isMostlySolidColor(best)) {
                            val candidateTimes = buildList {
                                if (durationUs > 20_000_000L) add(15_000_000L)
                                else if (durationUs > 4_000_000L) add(durationUs / 3L)
                                if (durationUs > 45_000_000L) add(30_000_000L)
                            }
                            for (t in candidateTimes) {
                                val alt = extractScaledFrame(retriever, t) ?: continue
                                if (!isMostlySolidColor(alt)) {
                                    best?.recycle()
                                    best = alt
                                    break
                                } else if (best == null) {
                                    best = alt
                                } else {
                                    alt.recycle()
                                }
                            }
                        }
                        if (best == null && bmp == null) {
                            best = retriever.frameAtTime?.let { scaleDownIfNeeded(it, MAX_THUMBNAIL_DIM_PX) }
                        }
                        best
                    } else {
                        retriever.embeddedPicture?.let { decodeSampledBitmap(it, MAX_THUMBNAIL_DIM_PX) }
                    }
                } catch (ce: CancellationException) {
                    throw ce
                } catch (_: Exception) {
                    null
                } finally {
                    runCatching { retriever.release() }
                }

                if (retrieved != null) {
                    if (bmp == null || !isMostlySolidColor(retrieved)) {
                        bmp?.recycle()
                        bmp = retrieved
                    } else {
                        retrieved.recycle()
                    }
                }
            }

            if (bmp != null && !bmp.isRecycled) {
                writeToDiskCache(appContext, uriString, bmp)
                ThumbnailCache.put(uriString, bmp)
            }
            bmp
        }
    }
}

@Composable
fun MediaThumbnail(
    uri: String,
    isVideo: Boolean,
    isStream: Boolean,
    contentDescription: String,
    shape: Shape,
    modifier: Modifier = Modifier
) {
    val appContext = LocalContext.current.applicationContext
    var bitmap by remember(uri) { mutableStateOf(ThumbnailCache.get(uri)) }

    LaunchedEffect(uri, isVideo, isStream) {
        if (bitmap == null && !isStream) {
            bitmap = extractBitmap(appContext, uri, isVideo, isStream)
        }
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        val loaded = bitmap
        if (loaded != null && !loaded.isRecycled) {
            Image(
                bitmap = loaded.asImageBitmap(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Image(
                painter = painterResource(
                    id = if (isVideo) R.drawable.img_video_empty_hero else R.drawable.img_audio_artwork_fallback
                ),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.15f),
                                Color.Black.copy(alpha = 0.45f)
                            )
                        )
                    )
            )
            Icon(
                imageVector = if (isVideo) Icons.Rounded.PlayArrow else Icons.Rounded.Audiotrack,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(26.dp)
            )
        }
    }
}
