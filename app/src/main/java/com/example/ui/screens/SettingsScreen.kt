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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.DecoderMode
import com.example.data.local.SubtitleEdgeStyle
import com.example.data.local.SubtitleFontStyle
import com.example.data.local.SubtitlePreferences
import com.example.data.local.ThemeMode
import com.example.data.local.UserPreferences
import com.example.ui.theme.MonoMetricsStyle
import java.util.Locale

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
    onUpdateSubtitlePrefs: ((SubtitlePreferences) -> SubtitlePreferences) -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenGestureControls: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
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
