package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.example.data.local.MediaItemEntity
import com.example.player.PlaybackUiState
import com.example.ui.components.MediaThumbnail
import com.example.ui.components.formatDuration
import com.example.ui.theme.MonoMetricsStyle
import java.util.Locale
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerScreen(
    playbackState: PlaybackUiState,
    currentMedia: MediaItemEntity?,
    onBack: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onAddToPlaylist: (MediaItemEntity) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipToQueueIndex: (Int) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleLike: () -> Unit,
    onSetSpeed: (Float) -> Unit,
    onSetSleepTimer: (Int?) -> Unit
) {
    var showQueueSheet by remember { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPos by remember { mutableLongStateOf(0L) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.16f),
                        MaterialTheme.colorScheme.background,
                        secondaryColor.copy(alpha = 0.10f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp)
                .align(Alignment.Center)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Collapse player")
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "NOW PLAYING",
                        style = MonoMetricsStyle,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (playbackState.formatBadge.isNotBlank()) {
                        Text(
                            text = playbackState.formatBadge,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = onOpenEqualizer,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("audio_player_eq_button")
                ) {
                    Icon(Icons.Rounded.GraphicEq, contentDescription = "Equalizer")
                }
            }

            // Elevated Album Artwork
            MediaThumbnail(
                uri = playbackState.currentUri.orEmpty(),
                isVideo = playbackState.isVideo,
                isStream = playbackState.currentUri?.startsWith("http") == true,
                contentDescription = playbackState.currentTitle,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth(0.76f)
                    .aspectRatio(1f)
                    .shadow(
                        elevation = if (playbackState.isPlaying) 28.dp else 10.dp,
                        shape = RoundedCornerShape(28.dp),
                        ambientColor = primaryColor,
                        spotColor = secondaryColor
                    )
            )

            // Reactive Studio Spectrum Visualizer
            ReactiveAudioVisualizer(
                isPlaying = playbackState.isPlaying,
                primary = primaryColor,
                secondary = secondaryColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
            )

            // Track Info & Like Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playbackState.currentTitle.ifBlank { "No Track Loaded" },
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = playbackState.currentArtist.ifBlank { "AetherPlay Studio" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = onToggleLike,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("audio_player_like_button")
                ) {
                    val liked = currentMedia?.isLiked == true
                    Icon(
                        imageVector = if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Like song",
                        tint = if (liked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Timeline Scrubber
            Column(modifier = Modifier.fillMaxWidth()) {
                val duration = playbackState.durationMs.coerceAtLeast(1L)
                val pos = if (isScrubbing) scrubPos else playbackState.currentPositionMs
                Slider(
                    value = pos.toFloat().coerceIn(0f, duration.toFloat()),
                    onValueChange = {
                        isScrubbing = true
                        scrubPos = it.toLong()
                    },
                    onValueChangeFinished = {
                        onSeekTo(scrubPos)
                        isScrubbing = false
                    },
                    valueRange = 0f..duration.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = primaryColor,
                        activeTrackColor = primaryColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatDuration(pos), style = MonoMetricsStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatDuration(playbackState.durationMs), style = MonoMetricsStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Main Transport Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleShuffle,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (playbackState.shuffleEnabled) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onSkipPrevious,
                    modifier = Modifier.size(54.dp)
                ) {
                    Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(34.dp))
                }
                Surface(
                    shape = CircleShape,
                    color = primaryColor,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onTogglePlayPause)
                        .testTag("audio_player_play_pause")
                ) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(18.dp)
                    )
                }
                IconButton(
                    onClick = onSkipNext,
                    modifier = Modifier.size(54.dp)
                ) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = "Next", modifier = Modifier.size(34.dp))
                }
                IconButton(
                    onClick = onCycleRepeat,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = if (playbackState.repeatMode == Player.REPEAT_MODE_ONE) {
                            Icons.Rounded.RepeatOne
                        } else {
                            Icons.Rounded.Repeat
                        },
                        contentDescription = "Repeat mode",
                        tint = if (playbackState.repeatMode != Player.REPEAT_MODE_OFF) {
                            primaryColor
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            // Quick Utility Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showSpeedSheet = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Speed, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(String.format(Locale.US, "%.2fx", playbackState.playbackSpeed), style = MonoMetricsStyle)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            val next = if (playbackState.sleepTimerRemainingMs == null) 30 else null
                            onSetSleepTimer(next)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Bedtime, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = playbackState.sleepTimerRemainingMs?.let { formatDuration(it) } ?: "Sleep",
                            style = MonoMetricsStyle
                        )
                    }
                }
                if (currentMedia != null) {
                    IconButton(onClick = { onAddToPlaylist(currentMedia) }) {
                        Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = "Add to playlist")
                    }
                }
                IconButton(
                    onClick = { showQueueSheet = true },
                    modifier = Modifier.testTag("audio_player_queue_button")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = "Up next queue")
                }
            }
        }
    }

    if (showQueueSheet) {
        ModalBottomSheet(onDismissRequest = { showQueueSheet = false }) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Up Next Queue (${playbackState.queue.size})",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
                LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                    itemsIndexed(playbackState.queue) { index, item ->
                        val active = index == playbackState.currentQueueIndex
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = item.title,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                    color = if (active) primaryColor else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            supportingContent = { Text(item.artist) },
                            trailingContent = {
                                if (item.durationMs > 0L) {
                                    Text(formatDuration(item.durationMs), style = MonoMetricsStyle)
                                }
                            },
                            modifier = Modifier.clickable {
                                onSkipToQueueIndex(index)
                                showQueueSheet = false
                            }
                        )
                    }
                }
            }
        }
    }

    if (showSpeedSheet) {
        ModalBottomSheet(onDismissRequest = { showSpeedSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Playback Speed: ${String.format(Locale.US, "%.2fx", playbackState.playbackSpeed)}",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Start
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf(0.5f, 0.8f, 1.0f, 1.25f, 1.5f, 2.0f)) { spd ->
                        FilterChip(
                            selected = kotlin.math.abs(playbackState.playbackSpeed - spd) < 0.05f,
                            onClick = { onSetSpeed(spd) },
                            label = { Text("${spd}x") }
                        )
                    }
                }
                Slider(
                    value = playbackState.playbackSpeed,
                    onValueChange = onSetSpeed,
                    valueRange = 0.5f..2.5f
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ReactiveAudioVisualizer(
    isPlaying: Boolean,
    primary: Color,
    secondary: Color,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "spectrum")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = modifier) {
        val barCount = 28
        val totalWidth = size.width
        val gap = 5.dp.toPx()
        val barWidth = ((totalWidth - gap * (barCount - 1)) / barCount).coerceAtLeast(3f)

        for (i in 0 until barCount) {
            val amp = if (isPlaying) {
                val wave = sin(phase + i * 0.45f) * 0.4f + sin(phase * 1.7f - i * 0.25f) * 0.35f
                (0.28f + kotlin.math.abs(wave)).coerceIn(0.15f, 1.0f)
            } else {
                0.14f
            }
            val barH = size.height * amp
            val x = i * (barWidth + gap)
            val y = (size.height - barH) / 2f
            val t = i.toFloat() / barCount.toFloat()
            val barColor = Color(
                red = primary.red + (secondary.red - primary.red) * t,
                green = primary.green + (secondary.green - primary.green) * t,
                blue = primary.blue + (secondary.blue - primary.blue) * t,
                alpha = if (isPlaying) 0.9f else 0.35f
            )
            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
