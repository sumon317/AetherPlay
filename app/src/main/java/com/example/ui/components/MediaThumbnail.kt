package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.LruCache
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
import kotlin.math.max

private const val MAX_THUMBNAIL_DIM_PX = 512
private val thumbnailExtractorSemaphore = Semaphore(permits = 2)

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

        thumbnailExtractorSemaphore.withPermit {
            ThumbnailCache.get(uriString)?.let { return@withPermit it }
            val retriever = MediaMetadataRetriever()
            val bmp = try {
                retriever.setDataSource(appContext, uri)
                if (isVideo) {
                    val raw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                        retriever.getScaledFrameAtTime(
                            2_000_000L,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                            MAX_THUMBNAIL_DIM_PX,
                            MAX_THUMBNAIL_DIM_PX
                        )
                    } else {
                        null
                    } ?: retriever.getFrameAtTime(2_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        ?: retriever.frameAtTime
                    raw?.let { scaleDownIfNeeded(it, MAX_THUMBNAIL_DIM_PX) }
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
            if (bmp != null) {
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

    LaunchedEffect(uri) {
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
