package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Loop
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import com.example.data.local.SubtitleEdgeStyle
import com.example.data.local.SubtitleFontStyle
import com.example.data.local.SubtitlePreferences
import com.example.data.local.UserPreferences
import com.example.player.PlaybackUiState
import com.example.player.TrackOption
import com.example.ui.components.formatDuration
import com.example.ui.components.formatSignedDeltaSeconds
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.MonoMetricsStyle
import com.example.ui.theme.PlusJakartaSansFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.viewmodel.AspectRatioMode
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.abs

private enum class GestureHudType { BRIGHTNESS, VOLUME, SEEK, DOUBLE_TAP, BOOST_2X, ZOOM }

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    controller: MediaController?,
    playbackState: PlaybackUiState,
    preferences: UserPreferences,
    subtitleText: String,
    isInPipMode: Boolean,
    onBack: () -> Unit,
    onEnterPip: () -> Unit,
    onSwitchToAudioOnly: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenGestureControls: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekBy: (Long) -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSetSpeed: (Float) -> Unit,
    onSelectAudioTrack: (TrackOption) -> Unit,
    onSelectSubtitleTrack: (TrackOption?) -> Unit,
    onAttachExternalSubtitle: (Uri) -> Unit,
    onUpdateSubtitlePrefs: ((SubtitlePreferences) -> SubtitlePreferences) -> Unit,
    onSetSleepTimer: (Int?) -> Unit,
    onToggleAbLoop: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val view = LocalView.current
    val isDarkTheme = LocalIsDarkTheme.current
    val audioManager = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    var showControls by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var aspectMode by remember { mutableStateOf(AspectRatioMode.FIT) }
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var isLandscape by remember { mutableStateOf(false) }

    var showTracksSheet by remember { mutableStateOf(false) }
    var tracksSheetInitialTab by remember { mutableIntStateOf(0) }
    var showSpeedSheet by remember { mutableStateOf(false) }

    // Gesture transient HUD state
    var activeHudType by remember { mutableStateOf<GestureHudType?>(null) }
    var hudLabel by remember { mutableStateOf("") }
    var hudFraction by remember { mutableFloatStateOf(0.5f) }
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPositionMs by remember { mutableLongStateOf(0L) }
    var speedBeforeBoost by remember { mutableFloatStateOf(1.0f) }

    val subtitleFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onAttachExternalSubtitle(uri)
    }

    // Manage system bars visibility & white icon legibility over dark cinema background
    DisposableEffect(showControls, isInPipMode) {
        val window = activity?.window
        if (window != null && !isInPipMode) {
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = false
            if (showControls) {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            } else {
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose { }
    }

    DisposableEffect(isDarkTheme) {
        onDispose {
            activity?.window?.let { window ->
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !isDarkTheme
                insetsController.isAppearanceLightNavigationBars = !isDarkTheme
                insetsController.show(WindowInsetsCompat.Type.systemBars())
                val lp = window.attributes
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                window.attributes = lp
            }
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Auto-hide controls while playing
    LaunchedEffect(showControls, playbackState.isPlaying, isLocked, showTracksSheet, showSpeedSheet) {
        if (showControls && playbackState.isPlaying && !showTracksSheet && !showSpeedSheet) {
            delay(4000L)
            showControls = false
        }
    }

    // Auto-hide transient gesture HUD
    LaunchedEffect(activeHudType, hudLabel) {
        if (activeHudType != null && activeHudType != GestureHudType.BOOST_2X && !isScrubbing) {
            delay(1100L)
            activeHudType = null
        }
    }

    BackHandler {
        if (isLocked) {
            showControls = true
        } else {
            onBack()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val containerWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val containerHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)

        val videoAspect = if (playbackState.videoWidth > 0 && playbackState.videoHeight > 0) {
            playbackState.videoWidth.toFloat() / playbackState.videoHeight.toFloat()
        } else 16f / 9f

        val surfaceModifier = when (aspectMode) {
            AspectRatioMode.FIT -> Modifier
                .align(Alignment.Center)
                .aspectRatio(videoAspect, matchHeightConstraintsFirst = false)
            AspectRatioMode.FILL -> Modifier.fillMaxSize()
            AspectRatioMode.CROP_16_9 -> Modifier
                .align(Alignment.Center)
                .aspectRatio(16f / 9f)
            AspectRatioMode.CROP_4_3 -> Modifier
                .align(Alignment.Center)
                .aspectRatio(4f / 3f)
            AspectRatioMode.STRETCH -> Modifier.fillMaxSize()
        }.graphicsLayer {
            scaleX = zoomScale
            scaleY = zoomScale
        }

        if (controller != null) {
            PlayerSurface(
                player = controller,
                surfaceType = if (preferences.useSurfaceView) {
                    SURFACE_TYPE_SURFACE_VIEW
                } else {
                    SURFACE_TYPE_TEXTURE_VIEW
                },
                modifier = surfaceModifier
            )
        }

        // Gesture Detection Layer (MX Player + Next Player + VLC style)
        if (!isInPipMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isLocked, preferences) {
                        if (isLocked) {
                            detectTapGestures(onTap = { showControls = !showControls })
                        } else {
                            detectTapGestures(
                                onTap = { showControls = !showControls },
                                onDoubleTap = { offset ->
                                    if (!preferences.doubleTapSeek) {
                                        onTogglePlayPause()
                                        return@detectTapGestures
                                    }
                                    val third = containerWidthPx / 3f
                                    when {
                                        offset.x < third -> {
                                            val delta = -preferences.defaultSeekSeconds * 1000L
                                            onSeekBy(delta)
                                            activeHudType = GestureHudType.DOUBLE_TAP
                                            hudLabel = "-${preferences.defaultSeekSeconds}s"
                                        }
                                        offset.x > third * 2f -> {
                                            val delta = preferences.defaultSeekSeconds * 1000L
                                            onSeekBy(delta)
                                            activeHudType = GestureHudType.DOUBLE_TAP
                                            hudLabel = "+${preferences.defaultSeekSeconds}s"
                                        }
                                        else -> onTogglePlayPause()
                                    }
                                },
                                onLongPress = {
                                    if (preferences.longPress2xSpeed) {
                                        speedBeforeBoost = playbackState.playbackSpeed
                                        onSetSpeed(2.0f)
                                        activeHudType = GestureHudType.BOOST_2X
                                        hudLabel = "2.0x Speed Boost"
                                    }
                                },
                                onPress = {
                                    tryAwaitRelease()
                                    if (activeHudType == GestureHudType.BOOST_2X) {
                                        onSetSpeed(speedBeforeBoost)
                                        activeHudType = null
                                    }
                                }
                            )
                        }
                    }
                    .pointerInput(isLocked, preferences) {
                        if (!isLocked) {
                            detectTransformGestures { _, _, zoom, _ ->
                                if (abs(zoom - 1f) > 0.01f) {
                                    zoomScale = (zoomScale * zoom).coerceIn(1.0f, 2.5f)
                                    activeHudType = GestureHudType.ZOOM
                                    hudLabel = "${(zoomScale * 100).toInt()}%"
                                }
                            }
                        }
                    }
                    .pointerInput(isLocked, preferences, playbackState.durationMs) {
                        if (!isLocked) {
                            var dragStartPos = Offset.Zero
                            var dragMode: GestureHudType? = null
                            var startBrightness = 0.5f
                            var startVolume = 5
                            var startSeekMs = 0L

                            detectDragGestures(
                                onDragStart = { start ->
                                    dragStartPos = start
                                    dragMode = null
                                    startBrightness = activity?.window?.attributes?.screenBrightness
                                        ?.takeIf { it >= 0f } ?: 0.5f
                                    startVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                    startSeekMs = playbackState.currentPositionMs
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val dx = change.position.x - dragStartPos.x
                                    val dy = change.position.y - dragStartPos.y

                                    if (dragMode == null && (abs(dx) > 18f || abs(dy) > 18f)) {
                                         dragMode = if (abs(dx) > abs(dy)) {
                                             if (preferences.gestureHorizontalSeek) GestureHudType.SEEK else null
                                         } else {
                                             if (dragStartPos.x < containerWidthPx / 2f) {
                                                 if (preferences.gestureBrightnessEnabled) GestureHudType.BRIGHTNESS else null
                                             } else {
                                                 if (preferences.gestureVolumeEnabled) GestureHudType.VOLUME else null
                                             }
                                         }
                                     }

                                     when (dragMode) {
                                         GestureHudType.BRIGHTNESS -> {
                                             val delta = (-dy / (containerHeightPx * 0.65f)) * preferences.brightnessSensitivity
                                             val next = (startBrightness + delta).coerceIn(0.02f, 1.0f)
                                             activity?.window?.let { w ->
                                                 val lp = w.attributes
                                                 lp.screenBrightness = next
                                                 w.attributes = lp
                                             }
                                             activeHudType = GestureHudType.BRIGHTNESS
                                             hudFraction = next
                                             hudLabel = "${(next * 100).toInt()}%"
                                         }
                                         GestureHudType.VOLUME -> {
                                             val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                                             val deltaSteps = (-dy / (containerHeightPx * 0.65f)) * maxVol * preferences.volumeSensitivity
                                             val nextVol = (startVolume + deltaSteps).toInt().coerceIn(0, maxVol)
                                             audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, nextVol, 0)
                                             val fraction = nextVol.toFloat() / maxVol.toFloat()
                                             activeHudType = GestureHudType.VOLUME
                                             hudFraction = fraction
                                             hudLabel = "${(fraction * 100).toInt()}%"
                                         }
                                         GestureHudType.SEEK -> {
                                             if (playbackState.durationMs > 0L) {
                                                 isScrubbing = true
                                                 val seekRangeMs = minOf(playbackState.durationMs, 180_000L)
                                                 val deltaMs = ((dx / containerWidthPx) * seekRangeMs * preferences.seekSensitivity).toLong()
                                                 val target = (startSeekMs + deltaMs).coerceIn(0L, playbackState.durationMs)
                                                 scrubPositionMs = target
                                                 activeHudType = GestureHudType.SEEK
                                                 hudLabel = "${formatDuration(target)} (${formatSignedDeltaSeconds(target - startSeekMs)})"
                                             }
                                         }
                                         else -> Unit
                                     }
                                },
                                onDragEnd = {
                                    if (dragMode == GestureHudType.SEEK && isScrubbing) {
                                        onSeekTo(scrubPositionMs)
                                    }
                                    isScrubbing = false
                                    if (activeHudType == GestureHudType.SEEK) {
                                        activeHudType = null
                                    }
                                    dragMode = null
                                },
                                onDragCancel = {
                                    isScrubbing = false
                                    activeHudType = null
                                    dragMode = null
                                }
                            )
                        }
                    }
            )
        }

        // Custom Subtitle Engine Overlay
        if (subtitleText.isNotBlank() && !isInPipMode) {
            SubtitleOverlay(
                text = subtitleText,
                prefs = preferences.subtitle,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (showControls && !isLocked) 130.dp else preferences.subtitle.bottomMarginDp.dp)
                    .padding(horizontal = 20.dp)
            )
        }

        // Gesture Feedback HUD Pills
        AnimatedVisibility(
            visible = activeHudType != null && !isInPipMode,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            GestureFeedbackPill(
                type = activeHudType,
                label = hudLabel,
                fraction = hudFraction
            )
        }

        // Screen Locked Floating Unlock Button
        AnimatedVisibility(
            visible = isLocked && showControls && !isInPipMode,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 24.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.Black.copy(alpha = 0.78f),
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { isLocked = false }
                    .testTag("unlock_screen_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.LockOpen, contentDescription = "Unlock screen", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tap to Unlock", color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        // Main Video Player HUD Controls
        AnimatedVisibility(
            visible = showControls && !isLocked && !isInPipMode,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top Gradient Scrim & Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                            )
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("player_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = playbackState.currentTitle.ifBlank { "AetherPlay Video" },
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (playbackState.isHardwareAccelerated) "HW+" else "SW",
                                    style = MonoMetricsStyle,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }
                            if (playbackState.formatBadge.isNotBlank()) {
                                Text(
                                    text = playbackState.formatBadge,
                                    style = MonoMetricsStyle,
                                    color = Color.White.copy(alpha = 0.78f),
                                    maxLines = 1
                                )
                            }
                            if (playbackState.sleepTimerRemainingMs != null) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "ZZZ ${formatDuration(playbackState.sleepTimerRemainingMs)}",
                                        style = MonoMetricsStyle,
                                        color = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                    IconButton(onClick = onSwitchToAudioOnly) {
                        Icon(Icons.Rounded.Headphones, contentDescription = "Background audio mode", tint = Color.White)
                    }
                    IconButton(onClick = onOpenEqualizer) {
                        Icon(Icons.Rounded.GraphicEq, contentDescription = "Equalizer", tint = Color.White)
                    }
                    IconButton(onClick = onEnterPip) {
                        Icon(Icons.Rounded.PictureInPictureAlt, contentDescription = "Picture in picture", tint = Color.White)
                    }
                    IconButton(
                        onClick = {
                            isLandscape = !isLandscape
                            activity?.requestedOrientation = if (isLandscape) {
                                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                            } else {
                                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                            }
                        }
                    ) {
                        Icon(Icons.Rounded.ScreenRotation, contentDescription = "Rotate screen", tint = Color.White)
                    }
                }

                // Center Transport Cluster
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onSeekBy(-preferences.defaultSeekSeconds * 1000L) },
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                    ) {
                        Icon(Icons.Rounded.Replay10, contentDescription = "Rewind", tint = Color.White, modifier = Modifier.size(30.dp))
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onTogglePlayPause)
                            .testTag("video_play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    IconButton(
                        onClick = { onSeekBy(preferences.defaultSeekSeconds * 1000L) },
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                    ) {
                        Icon(Icons.Rounded.Forward10, contentDescription = "Forward", tint = Color.White, modifier = Modifier.size(30.dp))
                    }
                }

                // Bottom Gradient Scrim & Controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.90f))
                            )
                        )
                        .navigationBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    // A-B loop indicator if set
                    if (playbackState.abLoopStartMs != null) {
                        Row(
                            modifier = Modifier.padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                val text = if (playbackState.abLoopEndMs == null) {
                                    "A-B Loop: A = ${formatDuration(playbackState.abLoopStartMs)} (Tap Loop again to set B)"
                                } else {
                                    "A-B Loop: ${formatDuration(playbackState.abLoopStartMs)} → ${formatDuration(playbackState.abLoopEndMs)}"
                                }
                                Text(
                                    text = text,
                                    style = MonoMetricsStyle,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    val displayedPos = if (isScrubbing) scrubPositionMs else playbackState.currentPositionMs
                    val duration = playbackState.durationMs.coerceAtLeast(1L)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = formatDuration(displayedPos),
                            style = MonoMetricsStyle,
                            color = Color.White
                        )
                        Slider(
                            value = displayedPos.toFloat().coerceIn(0f, duration.toFloat()),
                            onValueChange = {
                                isScrubbing = true
                                scrubPositionMs = it.toLong()
                            },
                            onValueChangeFinished = {
                                onSeekTo(scrubPositionMs)
                                isScrubbing = false
                            },
                            valueRange = 0f..duration.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = Color.White.copy(alpha = 0.28f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 10.dp)
                                .testTag("video_seek_slider")
                        )
                        Text(
                            text = formatDuration(playbackState.durationMs),
                            style = MonoMetricsStyle,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                isLocked = true
                                showControls = false
                            }
                        ) {
                            Icon(Icons.Rounded.Lock, contentDescription = "Lock gestures", tint = Color.White)
                        }
                        IconButton(onClick = onSkipPrevious) {
                            Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous", tint = Color.White)
                        }
                        IconButton(
                            onClick = {
                                tracksSheetInitialTab = 0
                                showTracksSheet = true
                            },
                            modifier = Modifier.testTag("video_subtitles_button")
                        ) {
                            Icon(Icons.Rounded.Subtitles, contentDescription = "Subtitles and Audio Tracks", tint = Color.White)
                        }
                        IconButton(
                            onClick = { showSpeedSheet = true },
                            modifier = Modifier.testTag("video_speed_button")
                        ) {
                            Icon(Icons.Rounded.Speed, contentDescription = "Speed, Timer and Loop", tint = Color.White)
                        }
                        IconButton(
                            onClick = {
                                val nextOrdinal = (aspectMode.ordinal + 1) % AspectRatioMode.entries.size
                                aspectMode = AspectRatioMode.entries[nextOrdinal]
                                zoomScale = 1f
                                activeHudType = GestureHudType.ZOOM
                                hudLabel = aspectMode.label
                            }
                        ) {
                            Icon(Icons.Rounded.AspectRatio, contentDescription = "Aspect ratio", tint = Color.White)
                        }
                        IconButton(onClick = onSkipNext) {
                            Icon(Icons.Rounded.SkipNext, contentDescription = "Next", tint = Color.White)
                        }
                    }
                }
            }
        }
    }

    // Audio / Subtitles & Subtitle Customization Sheet
    if (showTracksSheet) {
        VideoTracksAndSubtitlesSheet(
            initialTab = tracksSheetInitialTab,
            playbackState = playbackState,
            subtitlePrefs = preferences.subtitle,
            onDismiss = { showTracksSheet = false },
            onSelectAudioTrack = onSelectAudioTrack,
            onSelectSubtitleTrack = onSelectSubtitleTrack,
            onPickExternalSubtitle = { subtitleFileLauncher.launch(arrayOf("*/*")) },
            onUpdateSubtitlePrefs = onUpdateSubtitlePrefs
        )
    }

    // Speed / Sleep Timer / A-B Loop Sheet
    if (showSpeedSheet) {
        VideoToolsSheet(
            playbackState = playbackState,
            aspectMode = aspectMode,
            onDismiss = { showSpeedSheet = false },
            onSetSpeed = onSetSpeed,
            onSetSleepTimer = onSetSleepTimer,
            onToggleAbLoop = onToggleAbLoop,
            onSelectAspect = {
                aspectMode = it
                zoomScale = 1f
            },
            onOpenGestureControls = {
                showSpeedSheet = false
                onOpenGestureControls()
            }
        )
    }
}

@Composable
private fun SubtitleOverlay(
    text: String,
    prefs: SubtitlePreferences,
    modifier: Modifier = Modifier
) {
    val fontFamily = when (prefs.fontStyle) {
        SubtitleFontStyle.SANS -> PlusJakartaSansFontFamily
        SubtitleFontStyle.MONO -> JetBrainsMonoFontFamily
        SubtitleFontStyle.DISPLAY -> SpaceGroteskFontFamily
    }
    val textColor = Color(prefs.textColorHex)
    val bgColor = when (prefs.edgeStyle) {
        SubtitleEdgeStyle.BOXED -> Color.Black.copy(alpha = prefs.backgroundOpacity.coerceIn(0.15f, 0.95f))
        else -> Color.Transparent
    }
    val shadow = when (prefs.edgeStyle) {
        SubtitleEdgeStyle.DROP_SHADOW, SubtitleEdgeStyle.OUTLINE -> Shadow(
            color = Color.Black.copy(alpha = 0.92f),
            offset = Offset(2f, 2f),
            blurRadius = if (prefs.edgeStyle == SubtitleEdgeStyle.OUTLINE) 4f else 8f
        )
        else -> null
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Text(
            text = text,
            color = textColor,
            textAlign = TextAlign.Center,
            style = TextStyle(
                fontFamily = fontFamily,
                fontSize = prefs.fontSizeSp.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = (prefs.fontSizeSp * 1.32f).sp,
                shadow = shadow
            ),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun GestureFeedbackPill(
    type: GestureHudType?,
    label: String,
    fraction: Float
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.Black.copy(alpha = 0.78f),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val icon = when (type) {
                    GestureHudType.BRIGHTNESS -> Icons.Rounded.Brightness6
                    GestureHudType.VOLUME -> Icons.Rounded.VolumeUp
                    GestureHudType.SEEK -> if (label.contains("-")) Icons.Rounded.FastRewind else Icons.Rounded.FastForward
                    GestureHudType.DOUBLE_TAP -> if (label.startsWith("-")) Icons.Rounded.Replay10 else Icons.Rounded.Forward10
                    GestureHudType.BOOST_2X -> Icons.Rounded.FastForward
                    GestureHudType.ZOOM -> Icons.Rounded.AspectRatio
                    null -> Icons.Rounded.PlayArrow
                }
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = label,
                    style = MonoMetricsStyle.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
            if (type == GestureHudType.BRIGHTNESS || type == GestureHudType.VOLUME) {
                LinearProgressIndicator(
                    progress = { fraction.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .width(130.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.White.copy(alpha = 0.25f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoTracksAndSubtitlesSheet(
    initialTab: Int,
    playbackState: PlaybackUiState,
    subtitlePrefs: SubtitlePreferences,
    onDismiss: () -> Unit,
    onSelectAudioTrack: (TrackOption) -> Unit,
    onSelectSubtitleTrack: (TrackOption?) -> Unit,
    onPickExternalSubtitle: () -> Unit,
    onUpdateSubtitlePrefs: ((SubtitlePreferences) -> SubtitlePreferences) -> Unit
) {
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Subtitles") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Style & Sync") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Audio Tracks") })
            }

            when (selectedTab) {
                0 -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilledTonalButton(
                                onClick = onPickExternalSubtitle,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("load_external_subtitle_button")
                            ) {
                                Icon(Icons.Rounded.UploadFile, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Load External Subtitle (.srt, .vtt, .ass, .ttml)")
                            }
                        }
                        item {
                            ListItem(
                                headlineContent = { Text("Off (Disable Subtitles)") },
                                leadingContent = {
                                    RadioButton(
                                        selected = !playbackState.subtitlesEnabled,
                                        onClick = { onSelectSubtitleTrack(null) }
                                    )
                                },
                                modifier = Modifier.clickable { onSelectSubtitleTrack(null) }
                            )
                        }
                        items(playbackState.subtitleTracks) { track ->
                            ListItem(
                                headlineContent = { Text(track.label) },
                                supportingContent = track.codec?.let { { Text(it) } },
                                leadingContent = {
                                    RadioButton(
                                        selected = playbackState.subtitlesEnabled && track.isSelected,
                                        onClick = { onSelectSubtitleTrack(track) }
                                    )
                                },
                                modifier = Modifier.clickable { onSelectSubtitleTrack(track) }
                            )
                        }
                        if (playbackState.subtitleTracks.isEmpty() && playbackState.externalSubtitleUri == null) {
                            item {
                                Text(
                                    text = "No embedded subtitle tracks detected. Load an external .srt, .vtt, or .ass file above.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
                1 -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            // Live Preview Box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF0A0E17))
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                SubtitleOverlay(
                                    text = "Sample Subtitle Preview\nCustom Engine Ready",
                                    prefs = subtitlePrefs
                                )
                            }
                        }
                        item {
                            Text("Font Size: ${subtitlePrefs.fontSizeSp.toInt()} sp", style = MaterialTheme.typography.labelLarge)
                            Slider(
                                value = subtitlePrefs.fontSizeSp,
                                onValueChange = { size -> onUpdateSubtitlePrefs { it.copy(fontSizeSp = size) } },
                                valueRange = 12f..36f
                            )
                        }
                        item {
                            val delaySec = subtitlePrefs.syncDelayMs / 1000f
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Sync Delay: ${String.format(Locale.US, "%+.1fs", delaySec)}",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                OutlinedButton(
                                    onClick = { onUpdateSubtitlePrefs { it.copy(syncDelayMs = 0L) } }
                                ) {
                                    Text("Reset 0.0s")
                                }
                            }
                            Slider(
                                value = delaySec,
                                onValueChange = { sec ->
                                    val snapped = (kotlin.math.round(sec * 10f) / 10f)
                                    onUpdateSubtitlePrefs { it.copy(syncDelayMs = (snapped * 1000L).toLong()) }
                                },
                                valueRange = -5.0f..5.0f
                            )
                        }
                        item {
                            Text("Font Family", style = MaterialTheme.typography.labelLarge)
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(SubtitleFontStyle.entries) { style ->
                                    FilterChip(
                                        selected = subtitlePrefs.fontStyle == style,
                                        onClick = { onUpdateSubtitlePrefs { it.copy(fontStyle = style) } },
                                        label = { Text(style.label) }
                                    )
                                }
                            }
                        }
                        item {
                            Text("Edge & Background", style = MaterialTheme.typography.labelLarge)
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(SubtitleEdgeStyle.entries) { edge ->
                                    FilterChip(
                                        selected = subtitlePrefs.edgeStyle == edge,
                                        onClick = { onUpdateSubtitlePrefs { it.copy(edgeStyle = edge) } },
                                        label = { Text(edge.label) }
                                    )
                                }
                            }
                        }
                        item {
                            Text("Text Color", style = MaterialTheme.typography.labelLarge)
                            Spacer(modifier = Modifier.height(8.dp))
                            val colors = listOf(
                                0xFFFFFFFFL to "White",
                                0xFFFFE066L to "Cinema Gold",
                                0xFF00E5FFL to "Cyan",
                                0xFF69F0AEL to "Mint"
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(colors) { (hex, label) ->
                                    FilterChip(
                                        selected = subtitlePrefs.textColorHex == hex,
                                        onClick = { onUpdateSubtitlePrefs { it.copy(textColorHex = hex) } },
                                        label = { Text(label) },
                                        leadingIcon = {
                                            Box(
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(hex))
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                2 -> {
                    LazyColumn(contentPadding = PaddingValues(16.dp)) {
                        if (playbackState.audioTracks.isEmpty()) {
                            item {
                                Text(
                                    text = "Single default audio stream active.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        } else {
                            items(playbackState.audioTracks) { track ->
                                ListItem(
                                    headlineContent = { Text(track.label) },
                                    supportingContent = track.codec?.let { { Text(it) } },
                                    leadingContent = {
                                        Icon(Icons.Rounded.Audiotrack, contentDescription = null)
                                    },
                                    trailingContent = {
                                        if (track.isSelected) {
                                            Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    },
                                    modifier = Modifier.clickable { onSelectAudioTrack(track) }
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoToolsSheet(
    playbackState: PlaybackUiState,
    aspectMode: AspectRatioMode,
    onDismiss: () -> Unit,
    onSetSpeed: (Float) -> Unit,
    onSetSleepTimer: (Int?) -> Unit,
    onToggleAbLoop: () -> Unit,
    onSelectAspect: (AspectRatioMode) -> Unit,
    onOpenGestureControls: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Playback Speed: ${String.format(Locale.US, "%.2fx", playbackState.playbackSpeed)}",
                style = MaterialTheme.typography.titleMedium
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 2.5f)) { speed ->
                    FilterChip(
                        selected = abs(playbackState.playbackSpeed - speed) < 0.05f,
                        onClick = { onSetSpeed(speed) },
                        label = { Text("${speed}x") }
                    )
                }
            }
            Slider(
                value = playbackState.playbackSpeed,
                onValueChange = onSetSpeed,
                valueRange = 0.25f..3.0f
            )

            Text("Aspect Ratio", style = MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AspectRatioMode.entries) { mode ->
                    FilterChip(
                        selected = aspectMode == mode,
                        onClick = { onSelectAspect(mode) },
                        label = { Text(mode.label) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onToggleAbLoop,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Loop, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        when {
                            playbackState.abLoopStartMs == null -> "Set A-B Loop"
                            playbackState.abLoopEndMs == null -> "Set Loop B"
                            else -> "Clear Loop"
                        }
                    )
                }
                FilledTonalButton(
                    onClick = {
                        val nextTimer = if (playbackState.sleepTimerRemainingMs == null) 30 else null
                        onSetSleepTimer(nextTimer)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Bedtime, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (playbackState.sleepTimerRemainingMs == null) "Sleep 30m" else "Cancel Timer")
                }
            }
            OutlinedButton(
                onClick = onOpenGestureControls,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("player_gesture_settings_button")
            ) {
                Icon(Icons.Rounded.Gesture, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Gesture Sensitivity Settings")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
