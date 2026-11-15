package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.AudioDoubleTapAction
import com.example.data.local.AudioSwipeAction
import com.example.data.local.DecoderMode
import com.example.data.local.SubtitleEdgeStyle
import com.example.data.local.SubtitleFontStyle
import com.example.data.local.SubtitlePreferences
import com.example.data.local.ThemeMode
import com.example.data.local.UserPreferences
import com.example.ui.theme.MonoMetricsStyle
import java.util.Locale

private val DURATION_FILTER_PRESETS = listOf(0, 10, 30, 60, 120, 300)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferences: UserPreferences,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetDynamicColor: (Boolean) -> Unit,
    onSetDecoderMode: (DecoderMode) -> Unit,
    onSetUseSurfaceView: (Boolean) -> Unit,
    onSetResumePlayback: (Boolean) -> Unit,
    onSetBackgroundVideoAudio: (Boolean) -> Unit,
    onSetAutoPip: (Boolean) -> Unit,
    onSetRotateAudioArtwork: (Boolean) -> Unit,
    onSetAudioSwipeDownAction: (AudioSwipeAction) -> Unit,
    onSetAudioSwipeUpAction: (AudioSwipeAction) -> Unit,
    onSetAudioDoubleTapAction: (AudioDoubleTapAction) -> Unit,
    onSetMinVideoDurationSeconds: (Int) -> Unit,
    onSetMinAudioDurationSeconds: (Int) -> Unit,
    onUpdateSubtitlePrefs: ((SubtitlePreferences) -> SubtitlePreferences) -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenGestureControls: () -> Unit,
    modifier: Modifier = Modifier
) {
    var customDialogTarget by remember { mutableStateOf<String?>(null) }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.94f)
                ),
                title = {
                    Column {
                        Text("Studio Settings", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            text = "Decoder, Gestures, Subtitles & Audio DSP",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
            // Audio Studio Equalizer Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("5-Band Audio Equalizer", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Presets, Bass Boost, 3D Virtualizer & Preamp",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FilledTonalButton(
                            onClick = onOpenEqualizer,
                            modifier = Modifier.testTag("settings_open_eq_button")
                        ) {
                            Icon(Icons.Rounded.GraphicEq, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open EQ")
                        }
                    }
                }
            }

            // Appearance & Theme
            item {
                SettingsSectionCard(title = "Appearance & Theme", icon = Icons.Rounded.DarkMode) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ThemeMode.entries) { mode ->
                            FilterChip(
                                selected = preferences.themeMode == mode,
                                onClick = { onSetThemeMode(mode) },
                                label = { Text(mode.label) },
                                modifier = Modifier.testTag("theme_chip_${mode.name}")
                            )
                        }
                    }
                    SettingsToggleRow(
                        title = "Material You Dynamic Color",
                        subtitle = "Match system wallpaper palette on Android 12+",
                        checked = preferences.useDynamicColor,
                        onCheckedChange = onSetDynamicColor
                    )
                }
            }

            // Media Library Duration Filter
            item {
                SettingsSectionCard(title = "Media Library Filter", icon = Icons.Rounded.FilterList) {
                    Text(
                        text = "Hide short clips, voice notes, or status files under a chosen duration.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    DurationFilterControl(
                        title = "Minimum Video Duration",
                        selectedSeconds = preferences.minVideoDurationSeconds,
                        testTagPrefix = "min_video_dur",
                        onSelectSeconds = onSetMinVideoDurationSeconds,
                        onOpenCustom = { customDialogTarget = "video" }
                    )
                    DurationFilterControl(
                        title = "Minimum Audio Duration",
                        selectedSeconds = preferences.minAudioDurationSeconds,
                        testTagPrefix = "min_audio_dur",
                        onSelectSeconds = onSetMinAudioDurationSeconds,
                        onOpenCustom = { customDialogTarget = "audio" }
                    )
                }
            }

            // Hardware Acceleration & Playback Engine
            item {
                SettingsSectionCard(title = "Hardware Acceleration & Playback", icon = Icons.Rounded.Memory) {
                    DecoderMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSetDecoderMode(mode) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = preferences.decoderMode == mode,
                                onClick = { onSetDecoderMode(mode) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(mode.label, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    mode.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    SettingsToggleRow(
                        title = "Zero-Copy SurfaceView",
                        subtitle = "Best battery & HDR video performance (off uses TextureView)",
                        checked = preferences.useSurfaceView,
                        onCheckedChange = onSetUseSurfaceView
                    )
                    SettingsToggleRow(
                        title = "Resume Playback Position",
                        subtitle = "Continue movies & tracks right where you left off",
                        checked = preferences.resumePlayback,
                        onCheckedChange = onSetResumePlayback
                    )
                    SettingsToggleRow(
                        title = "Background Video-as-Audio",
                        subtitle = "Keep playing video audio seamlessly when switching apps",
                        checked = preferences.backgroundVideoAudio,
                        onCheckedChange = onSetBackgroundVideoAudio
                    )
                    SettingsToggleRow(
                        title = "Auto Picture-in-Picture",
                        subtitle = "Shrink active video into floating window on Home press",
                        checked = preferences.autoPip,
                        onCheckedChange = onSetAutoPip
                    )
                }
            }

            // Music Player Artwork & Gestures
            item {
                SettingsSectionCard(title = "Music Player & Artwork", icon = Icons.Rounded.Audiotrack) {
                    Text("Album Artwork Style", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = !preferences.rotateAudioArtwork,
                                onClick = { onSetRotateAudioArtwork(false) },
                                label = { Text("Still (Rounded)") },
                                modifier = Modifier.testTag("artwork_style_still")
                            )
                        }
                        item {
                            FilterChip(
                                selected = preferences.rotateAudioArtwork,
                                onClick = { onSetRotateAudioArtwork(true) },
                                label = { Text("Rotate (Round Vinyl)") },
                                modifier = Modifier.testTag("artwork_style_rotate")
                            )
                        }
                    }

                    Text("Swipe Down Action", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(AudioSwipeAction.entries) { action ->
                            FilterChip(
                                selected = preferences.audioSwipeDownAction == action,
                                onClick = { onSetAudioSwipeDownAction(action) },
                                label = { Text(action.label) },
                                modifier = Modifier.testTag("swipe_down_${action.name}")
                            )
                        }
                    }

                    Text("Swipe Up Action", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(AudioSwipeAction.entries) { action ->
                            FilterChip(
                                selected = preferences.audioSwipeUpAction == action,
                                onClick = { onSetAudioSwipeUpAction(action) },
                                label = { Text(action.label) },
                                modifier = Modifier.testTag("swipe_up_${action.name}")
                            )
                        }
                    }

                    Text("Double-Tap Artwork Action", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(AudioDoubleTapAction.entries) { action ->
                            FilterChip(
                                selected = preferences.audioDoubleTapAction == action,
                                onClick = { onSetAudioDoubleTapAction(action) },
                                label = { Text(action.label) },
                                modifier = Modifier.testTag("double_tap_${action.name}")
                            )
                        }
                    }
                }
            }

            // Gesture Controls Menu Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenGestureControls)
                        .testTag("settings_open_gestures_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.Gesture,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Gesture Controls", style = MaterialTheme.typography.titleMedium)
                            }
                            Text(
                                text = "Brightness ${String.format(Locale.US, "%.2fx", preferences.brightnessSensitivity)} • Volume ${String.format(Locale.US, "%.2fx", preferences.volumeSensitivity)} • Seek ${preferences.defaultSeekSeconds}s",
                                style = MonoMetricsStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        FilledTonalButton(
                            onClick = onOpenGestureControls,
                            modifier = Modifier.testTag("settings_open_gestures_button")
                        ) {
                            Text("Configure")
                        }
                    }
                }
            }

            // Subtitle Engine Defaults
            item {
                SettingsSectionCard(title = "Subtitle Engine Defaults", icon = Icons.Rounded.Subtitles) {
                    Text(
                        text = "Default Font Size: ${preferences.subtitle.fontSizeSp.toInt()} sp",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Slider(
                        value = preferences.subtitle.fontSizeSp,
                        onValueChange = { size -> onUpdateSubtitlePrefs { it.copy(fontSizeSp = size) } },
                        valueRange = 12f..36f
                    )
                    val delaySec = preferences.subtitle.syncDelayMs / 1000f
                    Text(
                        text = "Default Sync Offset: ${String.format(Locale.US, "%+.1fs", delaySec)}",
                        style = MonoMetricsStyle,
                        color = MaterialTheme.colorScheme.primary
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(SubtitleFontStyle.entries) { font ->
                            FilterChip(
                                selected = preferences.subtitle.fontStyle == font,
                                onClick = { onUpdateSubtitlePrefs { it.copy(fontStyle = font) } },
                                label = { Text(font.label) }
                            )
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(SubtitleEdgeStyle.entries) { edge ->
                            FilterChip(
                                selected = preferences.subtitle.edgeStyle == edge,
                                onClick = { onUpdateSubtitlePrefs { it.copy(edgeStyle = edge) } },
                                label = { Text(edge.label) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (customDialogTarget != null) {
        val isVideo = customDialogTarget == "video"
        val currentSec = if (isVideo) preferences.minVideoDurationSeconds else preferences.minAudioDurationSeconds
        CustomDurationFilterDialog(
            title = if (isVideo) "Minimum Video Duration" else "Minimum Audio Duration",
            initialSeconds = currentSec,
            onDismiss = { customDialogTarget = null },
            onApply = { newSeconds ->
                if (isVideo) {
                    onSetMinVideoDurationSeconds(newSeconds)
                } else {
                    onSetMinAudioDurationSeconds(newSeconds)
                }
                customDialogTarget = null
            }
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(10.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            content()
        }
    }
}

@Composable
private fun SettingsToggleRow(
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
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun DurationFilterControl(
    title: String,
    selectedSeconds: Int,
    testTagPrefix: String,
    onSelectSeconds: (Int) -> Unit,
    onOpenCustom: () -> Unit
) {
    val isCustom = selectedSeconds > 0 && selectedSeconds !in DURATION_FILTER_PRESETS
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(
                text = formatDurationFilterLabel(selectedSeconds),
                style = MonoMetricsStyle,
                color = if (selectedSeconds > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(DURATION_FILTER_PRESETS) { sec ->
                FilterChip(
                    selected = selectedSeconds == sec,
                    onClick = { onSelectSeconds(sec) },
                    label = { Text(if (sec == 0) "Off" else formatDurationChipLabel(sec)) },
                    modifier = Modifier.testTag("${testTagPrefix}_$sec")
                )
            }
            item {
                FilterChip(
                    selected = isCustom,
                    onClick = onOpenCustom,
                    label = {
                        Text(if (isCustom) "Custom (${formatDurationChipLabel(selectedSeconds)})" else "Custom...")
                    },
                    modifier = Modifier.testTag("${testTagPrefix}_custom")
                )
            }
        }
    }
}

@Composable
private fun CustomDurationFilterDialog(
    title: String,
    initialSeconds: Int,
    onDismiss: () -> Unit,
    onApply: (Int) -> Unit
) {
    val startInMinutes = initialSeconds >= 60 && initialSeconds % 60 == 0
    var useMinutes by remember(initialSeconds) { mutableStateOf(startInMinutes) }
    var inputText by remember(initialSeconds) {
        val displayVal = when {
            initialSeconds <= 0 -> "30"
            startInMinutes -> (initialSeconds / 60).toString()
            else -> initialSeconds.toString()
        }
        mutableStateOf(displayVal)
    }
    val parsed = inputText.trim().toIntOrNull()
    val resolvedSeconds = parsed?.let { value ->
        if (useMinutes) value * 60 else value
    }?.coerceIn(0, 3600)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Files shorter than this duration will be hidden from the library (0 turns the filter off; max 60 min).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { text ->
                        if (text.length <= 4 && text.all { it.isDigit() }) {
                            inputText = text
                        }
                    },
                    singleLine = true,
                    label = { Text(if (useMinutes) "Minutes" else "Seconds") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_duration_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !useMinutes,
                        onClick = { useMinutes = false },
                        label = { Text("Seconds") }
                    )
                    FilterChip(
                        selected = useMinutes,
                        onClick = { useMinutes = true },
                        label = { Text("Minutes") }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = resolvedSeconds != null,
                onClick = { resolvedSeconds?.let(onApply) },
                modifier = Modifier.testTag("custom_duration_apply")
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatDurationChipLabel(seconds: Int): String {
    return when {
        seconds <= 0 -> "Off"
        seconds % 60 == 0 -> "${seconds / 60}m"
        seconds > 60 -> "${seconds / 60}m ${seconds % 60}s"
        else -> "${seconds}s"
    }
}

private fun formatDurationFilterLabel(seconds: Int): String {
    return if (seconds <= 0) "Off (Show all)" else ">= ${formatDurationChipLabel(seconds)}"
}
