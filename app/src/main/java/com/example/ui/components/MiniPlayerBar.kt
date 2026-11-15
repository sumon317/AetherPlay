package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import com.example.player.PlaybackUiState
import com.example.ui.theme.MonoMetricsStyle
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun MiniPlayerBar(
    playbackState: PlaybackUiState,
    controller: MediaController?,
    isVisible: Boolean,
    onOpenPlayer: () -> Unit,
    onToggleFloatingVideo: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val dragOffsetX = remember(playbackState.currentUri) { Animatable(0f) }

    AnimatedVisibility(
        visible = isVisible && playbackState.hasMedia && playbackState.currentUri != null,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier
    ) {
        val progress = if (playbackState.durationMs > 0L) {
            (playbackState.currentPositionMs.toFloat() / playbackState.durationMs.toFloat()).coerceIn(0f, 1f)
        } else 0f

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .offset { IntOffset(dragOffsetX.value.roundToInt(), 0) }
                    .pointerInput(playbackState.currentUri) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                scope.launch {
                                    if (abs(dragOffsetX.value) > 220f) {
                                        onDismiss()
                                    }
                                    dragOffsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                                }
                            },
                            onDragCancel = {
                                scope.launch {
                                    dragOffsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                                }
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                scope.launch {
                                    dragOffsetX.snapTo(dragOffsetX.value + dragAmount)
                                }
                            }
                        )
                    }
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(onClick = onOpenPlayer)
                    .testTag("mini_player_bar"),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
                tonalElevation = 10.dp,
                shadowElevation = 10.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 10.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (playbackState.isVideo && controller != null) {
                            Box(
                                modifier = Modifier
                                    .width(78.dp)
                                    .aspectRatio(16f / 9f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.Black)
                            ) {
                                PlayerSurface(
                                    player = controller,
                                    surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        } else {
                            MediaThumbnail(
                                uri = playbackState.currentUri.orEmpty(),
                                isVideo = playbackState.isVideo,
                                isStream = playbackState.currentUri?.startsWith("http") == true,
                                contentDescription = "Now playing artwork",
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.size(46.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = playbackState.currentTitle.ifBlank { "Now Playing" },
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                            )
                            val timeMeta = if (playbackState.durationMs > 0L) {
                                "${formatDuration(playbackState.currentPositionMs)} / ${formatDuration(playbackState.durationMs)}"
                            } else ""
                            val subtitle = listOf(
                                timeMeta,
                                playbackState.currentArtist.takeIf { it.isNotBlank() && it != "Imported" } ?: "",
                                playbackState.formatBadge
                            ).filter { it.isNotBlank() }.joinToString(" • ")

                            Text(
                                text = subtitle.ifBlank { if (playbackState.isVideo) "Video Playing" else "Audio Playing" },
                                style = MonoMetricsStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (playbackState.isVideo) {
                            IconButton(
                                onClick = onToggleFloatingVideo,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("mini_player_float_toggle")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PictureInPictureAlt,
                                    contentDescription = "Float mini video window",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        IconButton(
                            onClick = onTogglePlayPause,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("mini_player_play_pause")
                        ) {
                            Icon(
                                imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (playbackState.queue.size > 1) {
                            IconButton(
                                onClick = onSkipNext,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("mini_player_next")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SkipNext,
                                    contentDescription = "Next item",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("mini_player_dismiss")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close mini player",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                }
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun FloatingMiniVideoOverlay(
    playbackState: PlaybackUiState,
    controller: MediaController?,
    isVisible: Boolean,
    onExpand: () -> Unit,
    onDockBack: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible && playbackState.hasMedia && playbackState.isVideo && controller != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val cardWidthDp = 224.dp
            val cardHeightDp = 126.dp
            val marginPx = with(density) { 16.dp.toPx() }
            val maxX = with(density) { (maxWidth - cardWidthDp).toPx() }.coerceAtLeast(marginPx)
            val maxY = with(density) { (maxHeight - cardHeightDp - 96.dp).toPx() }.coerceAtLeast(marginPx)

            var offsetX by remember { mutableFloatStateOf(maxX - marginPx) }
            var offsetY by remember { mutableFloatStateOf(maxY - marginPx) }

            val progress = if (playbackState.durationMs > 0L) {
                (playbackState.currentPositionMs.toFloat() / playbackState.durationMs.toFloat()).coerceIn(0f, 1f)
            } else 0f

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.Black,
                border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)),
                modifier = Modifier
                    .offset {
                        IntOffset(
                            offsetX.coerceIn(marginPx, maxX).roundToInt(),
                            offsetY.coerceIn(marginPx, maxY).roundToInt()
                        )
                    }
                    .size(width = cardWidthDp, height = cardHeightDp)
                    .shadow(16.dp, RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp))
                    .pointerInput(maxX, maxY) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            offsetX = (offsetX + dragAmount.x).coerceIn(marginPx, maxX)
                            offsetY = (offsetY + dragAmount.y).coerceIn(marginPx, maxY)
                        }
                    }
                    .clickable(onClick = onExpand)
                    .testTag("floating_mini_video_overlay")
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (controller != null) {
                        PlayerSurface(
                            player = controller,
                            surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Top subtle scrim + Dock / Expand / Close actions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Black.copy(alpha = 0.72f), Color.Transparent)
                                )
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDockBack,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ViewAgenda,
                                contentDescription = "Dock to bottom bar",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Row {
                            IconButton(
                                onClick = onExpand,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.OpenInFull,
                                    contentDescription = "Expand video player",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Close video",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Bottom Play/Pause pill + Title + Progress bar
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.80f))
                                )
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .clickable(onClick = onTogglePlayPause)
                            ) {
                                Icon(
                                    imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(6.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = playbackState.currentTitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.5.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color.White.copy(alpha = 0.25f)
                        )
                    }
                }
            }
        }
    }
}
