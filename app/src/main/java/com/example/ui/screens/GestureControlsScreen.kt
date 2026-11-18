package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.AudioDoubleTapAction
import com.example.data.local.AudioSwipeAction
import com.example.data.local.UserPreferences
import com.example.ui.theme.MonoMetricsStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private val SENSITIVITY_PRESETS = listOf(
    0.5f to "0.5x Fine",
    1.0f to "1.0x Balanced",
    1.5f to "1.5x Fast",
    2.0f to "2.0x Rapid"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureControlsScreen(
    preferences: UserPreferences,
    onBack: () -> Unit,
    onSetBrightnessEnabled: (Boolean) -> Unit,
    onSetVolumeEnabled: (Boolean) -> Unit,
    onSetBrightnessSensitivity: (Float) -> Unit,
    onSetVolumeSensitivity: (Float) -> Unit,
    onSetSeekSensitivity: (Float) -> Unit,
    onSetHorizontalSeek: (Boolean) -> Unit,
    onSetDoubleTapSeek: (Boolean) -> Unit,
    onSetDefaultSeekSeconds: (Int) -> Unit,
    onSetLongPress2x: (Boolean) -> Unit,
    onSetRotateAudioArtwork: (Boolean) -> Unit,
    onSetAudioSwipeDownAction: (AudioSwipeAction) -> Unit,
    onSetAudioSwipeUpAction: (AudioSwipeAction) -> Unit,
    onSetAudioDoubleTapAction: (AudioDoubleTapAction) -> Unit,
    onResetDefaults: () -> Unit
) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("gesture_settings_back")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column {
                        Text("Gesture Controls", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            text = "Tune swipe sensitivity & player touch zones",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onResetDefaults,
                        modifier = Modifier.testTag("gesture_reset_button")
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Reset gesture defaults")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = 112.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Visual Touch-Zone Diagram Card
            item {
                GestureZoneDiagramCard(preferences = preferences)
            }

            // Left Edge Brightness Slider Controls
            item {
                SensitivityControlCard(
                    title = "Brightness Swipe Slider",
                    subtitle = "Vertical swipe along the left half of the video screen",
                    icon = Icons.Rounded.Brightness6,
                    accentColor = MaterialTheme.colorScheme.primary,
                    enabled = preferences.gestureBrightnessEnabled,
                    onToggleEnabled = onSetBrightnessEnabled,
                    sensitivity = preferences.brightnessSensitivity,
                    onSensitivityChange = onSetBrightnessSensitivity,
                    toggleTag = "gesture_brightness_toggle",
                    sliderTag = "brightness_sensitivity_slider"
                )
            }

            // Right Edge Volume Slider Controls
            item {
                SensitivityControlCard(
                    title = "Volume Swipe Slider",
                    subtitle = "Vertical swipe along the right half of the video screen",
                    icon = Icons.Rounded.VolumeUp,
                    accentColor = MaterialTheme.colorScheme.secondary,
                    enabled = preferences.gestureVolumeEnabled,
                    onToggleEnabled = onSetVolumeEnabled,
                    sensitivity = preferences.volumeSensitivity,
                    onSensitivityChange = onSetVolumeSensitivity,
                    toggleTag = "gesture_volume_toggle",
                    sliderTag = "volume_sensitivity_slider"
                )
            }

            // Horizontal Seek Scrubbing Sensitivity
            item {
                SensitivityControlCard(
                    title = "Horizontal Seek Scrubbing",
                    subtitle = "Slide horizontally across the video to scrub timeline",
                    icon = Icons.Rounded.SwapHoriz,
                    accentColor = MaterialTheme.colorScheme.tertiary,
                    enabled = preferences.gestureHorizontalSeek,
                    onToggleEnabled = onSetHorizontalSeek,
                    sensitivity = preferences.seekSensitivity,
                    onSensitivityChange = onSetSeekSensitivity,
                    toggleTag = "gesture_seek_toggle",
                    sliderTag = "seek_sensitivity_slider"
                )
            }

            // Tap & Hold Gestures
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.TouchApp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Tap & Hold Actions", style = MaterialTheme.typography.titleMedium)
                        }

                        GestureSwitchRow(
                            title = "Double-Tap Left/Right Seek",
                            subtitle = "Jump backward or forward by ${preferences.defaultSeekSeconds}s",
                            checked = preferences.doubleTapSeek,
                            onCheckedChange = onSetDoubleTapSeek
                        )

                        if (preferences.doubleTapSeek) {
                            Text(
                                text = "Double-Tap Skip Interval",
                                style = MaterialTheme.typography.labelLarge
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(listOf(5, 10, 15, 30)) { sec ->
                                    FilterChip(
                                        selected = preferences.defaultSeekSeconds == sec,
                                        onClick = { onSetDefaultSeekSeconds(sec) },
                                        label = { Text("${sec}s") }
                                    )
                                }
                            }
                        }

                        GestureSwitchRow(
                            title = "Long-Press 2.0x Speed Boost",
                            subtitle = "Press and hold anywhere on video for instant 2.0x playback",
                            checked = preferences.longPress2xSpeed,
                            onCheckedChange = onSetLongPress2x
                        )
                    }
                }
            }

            // Audio Player Gestures & Artwork Style
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Album, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Music Player Gestures & Artwork", style = MaterialTheme.typography.titleMedium)
                        }

                        GestureSwitchRow(
                            title = "Rotating Vinyl Album Art",
                            subtitle = "Spin round vinyl cover art during playback (off shows rounded frame)",
                            checked = preferences.rotateAudioArtwork,
                            onCheckedChange = onSetRotateAudioArtwork
                        )

                        Text("Swipe Down Action", style = MaterialTheme.typography.labelLarge)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(AudioSwipeAction.entries) { action ->
                                FilterChip(
                                    selected = preferences.audioSwipeDownAction == action,
                                    onClick = { onSetAudioSwipeDownAction(action) },
                                    label = { Text(action.label) }
                                )
                            }
                        }

                        Text("Swipe Up Action", style = MaterialTheme.typography.labelLarge)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(AudioSwipeAction.entries) { action ->
                                FilterChip(
                                    selected = preferences.audioSwipeUpAction == action,
                                    onClick = { onSetAudioSwipeUpAction(action) },
                                    label = { Text(action.label) }
                                )
                            }
                        }

                        Text("Double-Tap Album Art", style = MaterialTheme.typography.labelLarge)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(AudioDoubleTapAction.entries) { action ->
                                FilterChip(
                                    selected = preferences.audioDoubleTapAction == action,
                                    onClick = { onSetAudioDoubleTapAction(action) },
                                    label = { Text(action.label) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GestureZoneDiagramCard(preferences: UserPreferences) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Player Gesture Map", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(106.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0A0E17)),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(
                            if (preferences.gestureBrightnessEnabled) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            } else {
                                Color.White.copy(alpha = 0.04f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.Brightness6,
                            contentDescription = null,
                            tint = if (preferences.gestureBrightnessEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color.White.copy(alpha = 0.35f)
                            }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (preferences.gestureBrightnessEnabled) {
                                "Brightness\n${formatSensitivity(preferences.brightnessSensitivity)}"
                            } else {
                                "Brightness\nOff"
                            },
                            style = MonoMetricsStyle,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(0.9f)
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.FastForward,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (preferences.gestureHorizontalSeek) {
                                "Seek\n${formatSensitivity(preferences.seekSensitivity)}"
                            } else {
                                "2x Hold"
                            },
                            style = MonoMetricsStyle,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(
                            if (preferences.gestureVolumeEnabled) {
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f)
                            } else {
                                Color.White.copy(alpha = 0.04f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.VolumeUp,
                            contentDescription = null,
                            tint = if (preferences.gestureVolumeEnabled) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                Color.White.copy(alpha = 0.35f)
                            }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (preferences.gestureVolumeEnabled) {
                                "Volume\n${formatSensitivity(preferences.volumeSensitivity)}"
                            } else {
                                "Volume\nOff"
                            },
                            style = MonoMetricsStyle,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SensitivityControlCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    enabled: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    sensitivity: Float,
    onSensitivityChange: (Float) -> Unit,
    toggleTag: String,
    sliderTag: String
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = accentColor)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggleEnabled,
                    modifier = Modifier.testTag(toggleTag)
                )
            }

            if (enabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Swipe Sensitivity", style = MaterialTheme.typography.labelLarge)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = accentColor.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = "${formatSensitivity(sensitivity)} • ${sensitivityDescriptor(sensitivity)}",
                            style = MonoMetricsStyle,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Slider(
                    value = sensitivity,
                    onValueChange = { raw ->
                        val snapped = ((raw * 20f).roundToInt() / 20f).coerceIn(0.25f, 2.5f)
                        onSensitivityChange(snapped)
                    },
                    valueRange = 0.25f..2.5f,
                    colors = SliderDefaults.colors(
                        thumbColor = accentColor,
                        activeTrackColor = accentColor
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(sliderTag)
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(SENSITIVITY_PRESETS) { (presetValue, presetLabel) ->
                        FilterChip(
                            selected = abs(sensitivity - presetValue) < 0.04f,
                            onClick = { onSensitivityChange(presetValue) },
                            label = { Text(presetLabel) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GestureSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun formatSensitivity(value: Float): String =
    String.format(Locale.US, "%.2fx", value)

private fun sensitivityDescriptor(value: Float): String = when {
    value < 0.75f -> "Ultra-Fine"
    value < 1.2f -> "Balanced"
    value < 1.75f -> "Fast"
    else -> "Rapid"
}
