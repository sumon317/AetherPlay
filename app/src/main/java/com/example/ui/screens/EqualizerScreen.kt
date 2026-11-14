package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.player.EqualizerBand
import com.example.player.EqualizerPreset
import com.example.player.EqualizerUiState
import com.example.ui.theme.MonoMetricsStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    uiState: EqualizerUiState,
    onBack: () -> Unit,
    onSetEnabled: (Boolean) -> Unit,
    onSelectPreset: (EqualizerPreset) -> Unit,
    onBandLevelChange: (Int, Int) -> Unit,
    onBassBoostChange: (Int) -> Unit,
    onVirtualizerChange: (Int) -> Unit,
    onLoudnessChange: (Int) -> Unit,
    onReset: () -> Unit
) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column {
                        Text("Audio Equalizer", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            text = if (uiState.isHardwareEffectBound) {
                                "Hardware DSP Active • ${uiState.selectedPreset}"
                            } else {
                                "Studio Profile • ${uiState.selectedPreset}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onReset,
                        modifier = Modifier.testTag("eq_reset_button")
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Reset equalizer")
                    }
                    Switch(
                        checked = uiState.enabled,
                        onCheckedChange = onSetEnabled,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .testTag("eq_master_switch")
                    )
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = 36.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(EqualizerUiState.BUILTIN_PRESETS) { preset ->
                        FilterChip(
                            selected = uiState.selectedPreset == preset.name,
                            onClick = { onSelectPreset(preset) },
                            label = { Text(preset.name) },
                            modifier = Modifier.testTag("eq_preset_${preset.name}")
                        )
                    }
                }
            }

            // Frequency Response Curve Card
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Frequency Curve", style = MaterialTheme.typography.titleMedium)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (uiState.enabled) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                }
                            ) {
                                Text(
                                    text = if (uiState.enabled) "ACTIVE" else "BYPASSED",
                                    style = MonoMetricsStyle,
                                    color = if (uiState.enabled) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        FrequencyResponseCurve(
                            bands = uiState.bands,
                            enabled = uiState.enabled,
                            primaryColor = MaterialTheme.colorScheme.primary,
                            secondaryColor = MaterialTheme.colorScheme.secondary,
                            gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                        )
                    }
                }
            }

            // 5-Band Vertical Studio Faders
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 18.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        uiState.bands.forEach { band ->
                            VerticalBandFader(
                                band = band,
                                enabled = uiState.enabled,
                                onLevelChange = { level -> onBandLevelChange(band.index, level) }
                            )
                        }
                    }
                }
            }

            // Spatial & Dynamics Controls
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text("Spatial & Dynamics", style = MaterialTheme.typography.titleMedium)

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Bass Boost", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = "${(uiState.bassBoostStrength / 10)}%",
                                    style = MonoMetricsStyle,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = uiState.bassBoostStrength.toFloat(),
                                onValueChange = { onBassBoostChange(it.toInt()) },
                                valueRange = 0f..1000f,
                                modifier = Modifier.testTag("eq_bass_slider")
                            )
                        }

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("3D Virtualizer", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = "${(uiState.virtualizerStrength / 10)}%",
                                    style = MonoMetricsStyle,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Slider(
                                value = uiState.virtualizerStrength.toFloat(),
                                onValueChange = { onVirtualizerChange(it.toInt()) },
                                valueRange = 0f..1000f,
                                modifier = Modifier.testTag("eq_virtualizer_slider")
                            )
                        }

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Preamp / Loudness Boost", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = String.format(Locale.US, "+%.1f dB", uiState.loudnessGainMb / 100f),
                                    style = MonoMetricsStyle,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            Slider(
                                value = uiState.loudnessGainMb.toFloat(),
                                onValueChange = { onLoudnessChange(it.toInt()) },
                                valueRange = 0f..1500f,
                                modifier = Modifier.testTag("eq_loudness_slider")
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FrequencyResponseCurve(
    bands: List<EqualizerBand>,
    enabled: Boolean,
    primaryColor: Color,
    secondaryColor: Color,
    gridColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val midY = size.height / 2f
        drawLine(
            color = gridColor,
            start = Offset(0f, midY),
            end = Offset(size.width, midY),
            strokeWidth = 1.5f
        )
        drawLine(
            color = gridColor.copy(alpha = 0.2f),
            start = Offset(0f, size.height * 0.2f),
            end = Offset(size.width, size.height * 0.2f),
            strokeWidth = 1f
        )
        drawLine(
            color = gridColor.copy(alpha = 0.2f),
            start = Offset(0f, size.height * 0.8f),
            end = Offset(size.width, size.height * 0.8f),
            strokeWidth = 1f
        )

        if (bands.isEmpty()) return@Canvas

        val points = bands.mapIndexed { idx, band ->
            val x = if (bands.size == 1) size.width / 2f else {
                (idx.toFloat() / (bands.size - 1).toFloat()) * (size.width - 24f) + 12f
            }
            val span = (band.maxLevelMb - band.minLevelMb).coerceAtLeast(1).toFloat()
            val norm = (band.levelMb - band.minLevelMb) / span
            val y = (1f - norm) * (size.height - 20f) + 10f
            Offset(x, y)
        }

        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 0 until points.lastIndex) {
                val p0 = points[i]
                val p1 = points[i + 1]
                val cx = (p0.x + p1.x) / 2f
                cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
            }
        }

        val lineColor = if (enabled) primaryColor else gridColor
        drawPath(
            path = path,
            brush = Brush.horizontalGradient(listOf(lineColor, if (enabled) secondaryColor else gridColor)),
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        )

        points.forEach { pt ->
            drawCircle(
                color = if (enabled) primaryColor else gridColor,
                radius = 5.dp.toPx(),
                center = pt
            )
        }
    }
}

@Composable
private fun VerticalBandFader(
    band: EqualizerBand,
    enabled: Boolean,
    onLevelChange: (Int) -> Unit
) {
    val activeColor = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val trackBg = MaterialTheme.colorScheme.surfaceContainerHighest
    val span = (band.maxLevelMb - band.minLevelMb).coerceAtLeast(1).toFloat()
    val norm = ((band.levelMb - band.minLevelMb) / span).coerceIn(0f, 1f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = String.format(Locale.US, "%+.1f", band.levelDb),
            style = MonoMetricsStyle,
            fontWeight = FontWeight.Bold,
            color = activeColor
        )
        Box(
            modifier = Modifier
                .width(52.dp)
                .height(180.dp)
                .pointerInput(band.index, band.minLevelMb, band.maxLevelMb) {
                    detectVerticalDragGestures { change, _ ->
                        change.consume()
                        val h = size.height.toFloat().coerceAtLeast(1f)
                        val yNorm = 1f - (change.position.y / h).coerceIn(0f, 1f)
                        val newLevel = (band.minLevelMb + yNorm * span).toInt()
                        onLevelChange(newLevel)
                    }
                }
                .testTag("eq_fader_${band.index}"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxHeight().width(28.dp)) {
                val trackW = 6.dp.toPx()
                val trackX = (size.width - trackW) / 2f
                drawRoundRect(
                    color = trackBg,
                    topLeft = Offset(trackX, 0f),
                    size = Size(trackW, size.height),
                    cornerRadius = CornerRadius(trackW / 2f, trackW / 2f)
                )

                val midY = size.height / 2f
                val thumbY = (1f - norm) * size.height
                val fillTop = minOf(midY, thumbY)
                val fillHeight = kotlin.math.abs(thumbY - midY)

                drawRoundRect(
                    color = activeColor,
                    topLeft = Offset(trackX, fillTop),
                    size = Size(trackW, fillHeight.coerceAtLeast(2f)),
                    cornerRadius = CornerRadius(trackW / 2f, trackW / 2f)
                )

                drawCircle(
                    color = activeColor,
                    radius = 10.dp.toPx(),
                    center = Offset(size.width / 2f, thumbY)
                )
            }
        }
        Text(
            text = band.label,
            style = MonoMetricsStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
