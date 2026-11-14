package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private object ThumbnailCache {
    private val cache = object : LruCache<String, Bitmap>(32) {}

    fun get(key: String): Bitmap? = cache.get(key)
    fun put(key: String, bitmap: Bitmap) {
        cache.put(key, bitmap)
    }
}

private suspend fun extractBitmap(
    context: Context,
    uriString: String,
    isVideo: Boolean,
    isStream: Boolean
): Bitmap? {
    if (isStream) return null
    ThumbnailCache.get(uriString)?.let { return it }
    return withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        val bmp = runCatching {
            retriever.setDataSource(context, Uri.parse(uriString))
            if (isVideo) {
                retriever.getFrameAtTime(2_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.frameAtTime
            } else {
                val artBytes = retriever.embeddedPicture
                artBytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
            }
        }.getOrNull()
        runCatching { retriever.release() }
        if (bmp != null) {
            ThumbnailCache.put(uriString, bmp)
        }
        bmp
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
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf(ThumbnailCache.get(uri)) }

    LaunchedEffect(uri) {
        if (bitmap == null && !isStream) {
            bitmap = extractBitmap(context, uri, isVideo, isStream)
        }
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        val loaded = bitmap
        if (loaded != null) {
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
