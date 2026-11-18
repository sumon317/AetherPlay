package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOn
import androidx.compose.material.icons.rounded.RepeatOneOn
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.ShuffleOn
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.example.data.local.AudioDoubleTapAction
import com.example.data.local.AudioSwipeAction
import com.example.data.local.MediaItemEntity
import com.example.data.local.UserPreferences
import com.example.player.PlaybackUiState
import com.example.player.ShuffleMode
import com.example.ui.components.MediaThumbnail
import com.example.ui.components.formatDuration
import com.example.ui.theme.MonoMetricsStyle
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerScreen(
    playbackState: PlaybackUiState,
    currentMedia: MediaItemEntity?,
    preferences: UserPreferences,
    likedAudioTracks: List<MediaItemEntity>,
    subtitleText: String,
    onBack: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onAddToPlaylist: (MediaItemEntity) -> Unit,
    onPlayQueue: (List<MediaItemEntity>, Int) -> Unit,
    onAttachExternalSubtitle: (Uri) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipToQueueIndex: (Int) -> Unit,
    onRemoveFromQueue: (Int) -> Unit = {},
    onToggleShuffle: () -> Unit,
    onSetShuffleMode: (ShuffleMode) -> Unit,
    onCycleRepeat: () -> Unit,
    onSetRepeatMode: (Int) -> Unit,
    onToggleLike: () -> Unit,
    onSetSpeed: (Float) -> Unit,
    onSetSleepTimer: (Int?) -> Unit
) {
    var showQueueSheet by remember { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showLikedSheet by remember { mutableStateOf(false) }
    var showLyricsSheet by remember { mutableStateOf(false) }
    var showHeartPulse by remember { mutableStateOf(false) }
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPos by remember { mutableLongStateOf(0L) }
    var vinylRotationDegrees by remember { mutableFloatStateOf(0f) }

    val subtitleFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onAttachExternalSubtitle(uri)
        }
    }

    LaunchedEffect(playbackState.isPlaying, preferences.rotateAudioArtwork) {
        if (!preferences.rotateAudioArtwork || !playbackState.isPlaying) return@LaunchedEffect
        var lastNanos = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                if (lastNanos != 0L) {
                    val deltaSec = (frameNanos - lastNanos) / 1_000_000_000f
                    vinylRotationDegrees = (vinylRotationDegrees + deltaSec * 24f) % 360f
                }
                lastNanos = frameNanos
            }
        }
    }

    LaunchedEffect(showHeartPulse) {
        if (showHeartPulse) {
            delay(650L)
            showHeartPulse = false
        }
    }

    fun handleSwipeAction(action: AudioSwipeAction) {
        when (action) {
            AudioSwipeAction.MINIMISE -> onBack()
            AudioSwipeAction.OPEN_QUEUE -> showQueueSheet = true
            AudioSwipeAction.OPEN_EQUALIZER -> onOpenEqualizer()
            AudioSwipeAction.OPEN_LIKED_SONGS -> showLikedSheet = true
        }
    }

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
            .pointerInput(preferences.audioSwipeUpAction, preferences.audioSwipeDownAction) {
                var accumulatedDrag = 0f
                var triggered = false
                detectVerticalDragGestures(
                    onDragStart = {
                        accumulatedDrag = 0f
                        triggered = false
                    },
                    onDragEnd = {
                        accumulatedDrag = 0f
                        triggered = false
                    },
                    onDragCancel = {
                        accumulatedDrag = 0f
                        triggered = false
                    },
                    onVerticalDrag = { _, dragAmount ->
                        accumulatedDrag += dragAmount
                        if (!triggered) {
                            if (accumulatedDrag < -80f) {
                                triggered = true
                                handleSwipeAction(preferences.audioSwipeUpAction)
                            } else if (accumulatedDrag > 80f) {
                                triggered = true
                                handleSwipeAction(preferences.audioSwipeDownAction)
                            }
                        }
                    }
                )
            }
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
                    val modeMeta = listOfNotNull(
                        playbackState.formatBadge.takeIf { it.isNotBlank() },
                        when (playbackState.shuffleMode) {
                            ShuffleMode.SMART -> "Smart Shuffle"
                            ShuffleMode.STANDARD -> "Shuffle"
                            ShuffleMode.OFF -> null
                        },
                        when (playbackState.repeatMode) {
                            Player.REPEAT_MODE_ALL -> "Repeat All"
                            Player.REPEAT_MODE_ONE -> "Repeat One"
                            else -> null
                        }
                    ).joinToString(" • ")
                    if (modeMeta.isNotBlank()) {
                        Text(
                            text = modeMeta,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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

            // Artwork (Rotating Vinyl or Still Rounded Card) with Double-Tap Gesture
            val artworkShape = if (preferences.rotateAudioArtwork) CircleShape else RoundedCornerShape(28.dp)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth(0.76f)
                    .aspectRatio(1f)
                    .pointerInput(preferences.audioDoubleTapAction) {
                        detectTapGestures(
                            onDoubleTap = {
                                when (preferences.audioDoubleTapAction) {
                                    AudioDoubleTapAction.LIKE_DISLIKE -> {
                                        onToggleLike()
                                        showHeartPulse = true
                                    }
                                    AudioDoubleTapAction.OPEN_SUBTITLES_LYRICS -> {
                                        showLyricsSheet = true
                                    }
                                }
                            }
                        )
                    }
                    .testTag("audio_player_artwork")
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            if (preferences.rotateAudioArtwork) {
                                rotationZ = vinylRotationDegrees
                            }
                        }
                        .shadow(
                            elevation = if (playbackState.isPlaying) 28.dp else 10.dp,
                            shape = artworkShape,
                            ambientColor = primaryColor,
                            spotColor = secondaryColor
                        )
                        .clip(artworkShape)
                        .then(
                            if (preferences.rotateAudioArtwork) {
                                Modifier.border(
                                    width = 6.dp,
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f),
                                    shape = CircleShape
                                )
                            } else {
                                Modifier
                            }
                        )
                ) {
                    MediaThumbnail(
                        uri = playbackState.currentUri.orEmpty(),
                        isVideo = playbackState.isVideo,
                        isStream = playbackState.currentUri?.startsWith("http") == true,
                        contentDescription = playbackState.currentTitle,
                        shape = artworkShape,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (preferences.rotateAudioArtwork) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.92f))
                                .border(2.dp, primaryColor.copy(alpha = 0.65f), CircleShape)
                        )
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = showHeartPulse,
                    enter = fadeIn() + scaleIn(initialScale = 0.6f),
                    exit = fadeOut() + scaleOut(targetScale = 1.25f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Favorite,
                            contentDescription = null,
                            tint = secondaryColor,
                            modifier = Modifier
                                .padding(18.dp)
                                .size(44.dp)
                        )
                    }
                }
            }

            // Live Lyric / Subtitle Banner when active
            if (subtitleText.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { showLyricsSheet = true }
                        .testTag("audio_player_lyric_banner")
                ) {
                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }

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
                val shuffleActive = playbackState.shuffleMode != ShuffleMode.OFF
                val shuffleTint = when (playbackState.shuffleMode) {
                    ShuffleMode.SMART -> secondaryColor
                    ShuffleMode.STANDARD -> primaryColor
                    ShuffleMode.OFF -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Box(contentAlignment = Alignment.Center) {
                    Surface(
                        shape = CircleShape,
                        color = if (shuffleActive) shuffleTint.copy(alpha = 0.16f) else Color.Transparent,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onToggleShuffle)
                            .testTag("audio_player_shuffle_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when (playbackState.shuffleMode) {
                                    ShuffleMode.SMART -> Icons.Rounded.AutoAwesome
                                    ShuffleMode.STANDARD -> Icons.Rounded.ShuffleOn
                                    ShuffleMode.OFF -> Icons.Rounded.Shuffle
                                },
                                contentDescription = when (playbackState.shuffleMode) {
                                    ShuffleMode.OFF -> "Enable Shuffle"
                                    ShuffleMode.STANDARD -> "Switch to Smart Shuffle"
                                    ShuffleMode.SMART -> "Smart Shuffle On, tap to turn off"
                                },
                                tint = shuffleTint,
                                modifier = Modifier.size(22.dp)
                            )
                            if (playbackState.shuffleMode == ShuffleMode.SMART) {
                                Text(
                                    text = "SMART",
                                    style = MonoMetricsStyle.copy(fontSize = 7.sp, fontWeight = FontWeight.Bold),
                                    color = secondaryColor,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 2.dp)
                                )
                            }
                        }
                    }
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
                val repeatActive = playbackState.repeatMode != Player.REPEAT_MODE_OFF
                val repeatTint = if (repeatActive) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                Surface(
                    shape = CircleShape,
                    color = if (repeatActive) primaryColor.copy(alpha = 0.16f) else Color.Transparent,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onCycleRepeat)
                        .testTag("audio_player_repeat_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when (playbackState.repeatMode) {
                                Player.REPEAT_MODE_ONE -> Icons.Rounded.RepeatOneOn
                                Player.REPEAT_MODE_ALL -> Icons.Rounded.RepeatOn
                                else -> Icons.Rounded.Repeat
                            },
                            contentDescription = when (playbackState.repeatMode) {
                                Player.REPEAT_MODE_OFF -> "Repeat Off, tap for Repeat All"
                                Player.REPEAT_MODE_ALL -> "Repeat All, tap for Repeat One"
                                else -> "Repeat One, tap for Off"
                            },
                            tint = repeatTint,
                            modifier = Modifier.size(22.dp)
                        )
                        if (playbackState.repeatMode != Player.REPEAT_MODE_OFF) {
                            Text(
                                text = if (playbackState.repeatMode == Player.REPEAT_MODE_ONE) "1" else "ALL",
                                style = MonoMetricsStyle.copy(fontSize = 7.sp, fontWeight = FontWeight.Bold),
                                color = primaryColor,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 2.dp)
                            )
                        }
                    }
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
                        .minimumInteractiveComponentSize()
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
                        .minimumInteractiveComponentSize()
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
                IconButton(
                    onClick = { showLyricsSheet = true },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("audio_player_lyrics_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lyrics,
                        contentDescription = "Lyrics and subtitles",
                        tint = if (playbackState.externalCues.isNotEmpty() || playbackState.activeCues.isNotEmpty()) {
                            primaryColor
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                if (currentMedia != null) {
                    IconButton(
                        onClick = { onAddToPlaylist(currentMedia) },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = "Add to playlist")
                    }
                }
                IconButton(
                    onClick = { showQueueSheet = true },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("audio_player_queue_button")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = "Up next queue")
                }
            }
        }
    }

    if (showQueueSheet) {
        val keyedQueue = remember(playbackState.queue) {
            val seen = mutableMapOf<String, Int>()
            playbackState.queue.map { item ->
                val occurrence = (seen[item.uri] ?: 0) + 1
                seen[item.uri] = occurrence
                "${item.uri}#$occurrence" to item
            }
        }
        ModalBottomSheet(onDismissRequest = { showQueueSheet = false }) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    Text(
                        text = "Up Next Queue (${playbackState.queue.size})",
                        style = MaterialTheme.typography.titleLarge
                    )
                    if (playbackState.queue.isNotEmpty()) {
                        Text(
                            text = "Tap to jump • Swipe left to remove from queue",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ShuffleMode.entries) { mode ->
                        FilterChip(
                            selected = playbackState.shuffleMode == mode,
                            onClick = { onSetShuffleMode(mode) },
                            leadingIcon = if (mode == ShuffleMode.SMART) {
                                { Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            label = { Text(mode.label) }
                        )
                    }
                    items(
                        listOf(
                            Player.REPEAT_MODE_OFF to "Repeat Off",
                            Player.REPEAT_MODE_ALL to "Repeat All",
                            Player.REPEAT_MODE_ONE to "Repeat One"
                        )
                    ) { (mode, label) ->
                        FilterChip(
                            selected = playbackState.repeatMode == mode,
                            onClick = { onSetRepeatMode(mode) },
                            label = { Text(label) }
                        )
                    }
                }
                LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                    itemsIndexed(keyedQueue, key = { _, pair -> pair.first }) { index, (_, item) ->
                        val active = index == playbackState.currentQueueIndex
                        val isSmartPick = item.uri in playbackState.smartPickUris
                        val currentIndex by rememberUpdatedState(index)
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                value == SwipeToDismissBoxValue.EndToStart
                            }
                        )

                        LaunchedEffect(dismissState.currentValue) {
                            if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                                onRemoveFromQueue(currentIndex)
                            }
                        }

                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            enableDismissFromEndToStart = true,
                            modifier = Modifier
                                .animateItem()
                                .testTag("queue_swipe_item_$index"),
                            backgroundContent = {
                                val isArmed = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart
                                val bgColor by animateColorAsState(
                                    targetValue = if (isArmed) {
                                        MaterialTheme.colorScheme.errorContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHigh
                                    },
                                    label = "queue_swipe_bg"
                                )
                                val contentColor by animateColorAsState(
                                    targetValue = if (isArmed) {
                                        MaterialTheme.colorScheme.onErrorContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    label = "queue_swipe_fg"
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(bgColor)
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Remove",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = contentColor
                                        )
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = "Remove from queue",
                                            tint = contentColor
                                        )
                                    }
                                }
                            }
                        ) {
                            ListItem(
                                headlineContent = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = item.title,
                                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                            color = if (active) primaryColor else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (isSmartPick) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = secondaryColor.copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = "Smart Pick",
                                                    style = MonoMetricsStyle.copy(fontSize = 10.sp),
                                                    color = secondaryColor,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                },
                                supportingContent = { Text(item.artist) },
                                colors = ListItemDefaults.colors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                ),
                                trailingContent = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (item.durationMs > 0L) {
                                            Text(formatDuration(item.durationMs), style = MonoMetricsStyle)
                                        }
                                        IconButton(
                                            onClick = { onRemoveFromQueue(currentIndex) },
                                            modifier = Modifier
                                                .minimumInteractiveComponentSize()
                                                .testTag("queue_remove_button_$index")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = "Remove from queue"
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier.clickable {
                                    onSkipToQueueIndex(currentIndex)
                                    showQueueSheet = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showLikedSheet) {
        ModalBottomSheet(onDismissRequest = { showLikedSheet = false }) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Liked Songs (${likedAudioTracks.size})",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
                if (likedAudioTracks.isEmpty()) {
                    Text(
                        text = "No liked songs yet. Tap or double-tap artwork to favourite a track.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)
                    )
                } else {
                    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                        itemsIndexed(likedAudioTracks) { index, item ->
                            val isCurrent = item.uri == playbackState.currentUri
                            ListItem(
                                headlineContent = {
                                    Text(
                                        text = item.title,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isCurrent) primaryColor else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                supportingContent = {
                                    Text(
                                        text = item.artist,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                leadingContent = {
                                    Icon(
                                        imageVector = Icons.Rounded.Favorite,
                                        contentDescription = null,
                                        tint = secondaryColor
                                    )
                                },
                                trailingContent = {
                                    if (item.durationMs > 0L) {
                                        Text(formatDuration(item.durationMs), style = MonoMetricsStyle)
                                    }
                                },
                                modifier = Modifier.clickable {
                                    onPlayQueue(likedAudioTracks, index)
                                    showLikedSheet = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showLyricsSheet) {
        ModalBottomSheet(onDismissRequest = { showLyricsSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Lyrics & Subtitles",
                        style = MaterialTheme.typography.titleLarge
                    )
                    FilledTonalButton(
                        onClick = { subtitleFileLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier.testTag("audio_load_lyrics_button")
                    ) {
                        Icon(Icons.Rounded.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Load LRC/SRT")
                    }
                }

                if (subtitleText.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = primaryColor.copy(alpha = 0.14f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }

                if (playbackState.externalCues.isNotEmpty()) {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 24.dp),
                        modifier = Modifier.height(280.dp)
                    ) {
                        items(playbackState.externalCues) { cue ->
                            val isActiveLine = playbackState.currentPositionMs in cue.startMs..cue.endMs
                            ListItem(
                                headlineContent = {
                                    Text(
                                        text = cue.text,
                                        fontWeight = if (isActiveLine) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isActiveLine) primaryColor else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                trailingContent = {
                                    Text(formatDuration(cue.startMs), style = MonoMetricsStyle)
                                },
                                modifier = Modifier.clickable {
                                    onSeekTo(cue.startMs)
                                }
                            )
                        }
                    }
                } else if (playbackState.activeCues.isNotEmpty()) {
                    Text(
                        text = playbackState.activeCues.joinToString("\n"),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                } else {
                    Text(
                        text = "No lyrics or subtitle file attached yet. Tap \"Load LRC/SRT\" to attach .lrc, .srt, or .vtt lyrics for this track.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
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
